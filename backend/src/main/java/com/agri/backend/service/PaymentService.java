package com.agri.backend.service;

import com.agri.backend.exception.ApiException;
import com.agri.backend.model.Order;
import com.agri.backend.model.Payment;
import com.agri.backend.model.ProduceListing;
import com.agri.backend.model.User;
import com.agri.backend.repository.OrderRepository;
import com.agri.backend.repository.PaymentRepository;
import com.agri.backend.repository.ProduceListingRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Payments for marketplace orders.
 *
 * REAL MODE  (RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET set): creates a Razorpay order; the browser opens Razorpay Checkout,
 *   which offers UPI (GPay, PhonePe, Paytm ... - these are UPI apps, not separate gateways), cards and net banking.
 *   The payment signature is verified on the server before an order is marked PAID.
 * DEMO MODE  (no keys): the app shows its own "demo checkout" and the server simply marks the order PAID so the whole
 *   buyer -> seller flow can be shown without real money. Demo payments are clearly labelled in the UI.
 *
 * Paying the seller out automatically needs Razorpay Route (marketplace split payments, KYC per seller). Until that
 * is approved, money lands in the platform account and payouts to the seller's UPI/bank are a settlement step.
 */
@Service
public class PaymentService {

    private static final Set<String> DEMO_METHODS = Set.of("UPI", "CARD", "NETBANKING");

    @Value("${razorpay.key-id:}")
    private String keyId;

    @Value("${razorpay.key-secret:}")
    private String keySecret;

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProduceListingRepository listingRepository;

    public PaymentService(OrderRepository orderRepository, PaymentRepository paymentRepository,
                          ProduceListingRepository listingRepository) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.listingRepository = listingRepository;
    }

    public boolean isDemoMode() {
        return keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank();
    }

    public String getKeyId() {
        return isDemoMode() ? "" : keyId;
    }

    @Transactional
    public Payment createPaymentOrder(Long orderId, User buyer) {
        Order order = ownedPendingOrder(orderId, buyer);

        var existing = paymentRepository.findFirstByOrderIdOrderByIdDesc(orderId);
        if (existing.isPresent() && existing.get().getStatus() == Payment.Status.CREATED) {
            return existing.get();
        }

        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setAmountRupees(order.getTotalAmountRupees());
        payment.setStatus(Payment.Status.CREATED);
        payment.setCreatedAt(LocalDateTime.now());

        if (isDemoMode()) {
            payment.setGatewayOrderId("demo_order_" + UUID.randomUUID());
        } else {
            try {
                RazorpayClient client = new RazorpayClient(keyId, keySecret);
                JSONObject options = new JSONObject();
                options.put("amount", Math.round(order.getTotalAmountRupees() * 100)); // paise
                options.put("currency", "INR");
                options.put("receipt", "order_" + order.getId());
                com.razorpay.Order rpOrder = client.orders.create(options);
                payment.setGatewayOrderId(rpOrder.get("id"));
            } catch (Exception e) {
                throw new ApiException(HttpStatus.BAD_GATEWAY,
                        "The payment gateway could not start this payment. Please try again in a moment.");
            }
        }
        return paymentRepository.save(payment);
    }

    /** Called after Razorpay Checkout succeeds. The signature is verified here - the browser's word is never trusted. */
    @Transactional
    public boolean verifyAndMarkPaid(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature, User buyer) {
        Payment payment = paymentRepository.findByGatewayOrderId(razorpayOrderId)
                .orElseThrow(() -> ApiException.notFound("No payment found for that order."));
        Order order = orderRepository.findById(payment.getOrderId()).orElseThrow();
        if (!buyer.getId().equals(order.getBuyerUserId())) throw ApiException.forbidden("That is not your order.");

        boolean valid;
        try {
            JSONObject attrs = new JSONObject();
            attrs.put("razorpay_order_id", razorpayOrderId);
            attrs.put("razorpay_payment_id", razorpayPaymentId);
            attrs.put("razorpay_signature", razorpaySignature);
            valid = !isDemoMode() && Utils.verifyPaymentSignature(attrs, keySecret);
        } catch (Exception e) {
            valid = false;
        }
        if (valid) {
            payment.setGatewayPaymentId(razorpayPaymentId);
            markPaid(payment, order, "ONLINE");
        } else {
            payment.setStatus(Payment.Status.FAILED);
            paymentRepository.save(payment);
        }
        return valid;
    }

    /** Demo-mode only: pretend-pay with the chosen method. Refused automatically when real keys are configured. */
    @Transactional
    public Payment confirmDemoPayment(Long orderId, String method, User buyer) {
        if (!isDemoMode()) throw ApiException.forbidden("Demo payments are disabled because live payment keys are configured.");
        String m = method == null ? "" : method.trim().toUpperCase();
        if (!DEMO_METHODS.contains(m)) throw ApiException.badRequest("Please choose UPI, card or net banking.");
        Order order = ownedPendingOrder(orderId, buyer);
        Payment payment = createPaymentOrder(order.getId(), buyer);
        payment.setGatewayPaymentId("demo_pay_" + UUID.randomUUID());
        markPaid(payment, order, m);
        return payment;
    }

    private Order ownedPendingOrder(Long orderId, User buyer) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("That order was not found."));
        if (!buyer.getId().equals(order.getBuyerUserId())) throw ApiException.forbidden("That is not your order.");
        if (order.getStatus() != Order.Status.PENDING_PAYMENT) {
            throw ApiException.conflict("This order is already " + order.getStatus().name().toLowerCase().replace('_', ' ') + ".");
        }
        return order;
    }

    private void markPaid(Payment payment, Order order, String method) {
        if (order.getStatus() == Order.Status.PAID) return; // idempotent

        ProduceListing listing = listingRepository.findById(order.getListingId()).orElse(null);
        if (listing != null) {
            double left = (listing.getQuantityKg() == null ? 0 : listing.getQuantityKg()) - order.getQuantityKg();
            listing.setQuantityKg(Math.max(0, left));
            if (left <= 0 && "AVAILABLE".equals(listing.getStatus())) listing.setStatus("SOLD_OUT");
            listingRepository.save(listing);
        }
        payment.setStatus(Payment.Status.SUCCESS);
        payment.setMethod(method);
        payment.setSettledAt(LocalDateTime.now());
        paymentRepository.save(payment);

        order.setStatus(Order.Status.PAID);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);
    }
}
