package com.agri.backend.controller;

import com.agri.backend.dto.ApiDtos.BuyerOrderView;
import com.agri.backend.exception.ApiException;
import com.agri.backend.model.Order;
import com.agri.backend.model.Payment;
import com.agri.backend.model.ProduceListing;
import com.agri.backend.model.User;
import com.agri.backend.repository.OrderRepository;
import com.agri.backend.repository.ProduceListingRepository;
import com.agri.backend.security.AuthInterceptor;
import com.agri.backend.service.OrderViewService;
import com.agri.backend.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Buyer checkout:
 *   1. POST /api/orders                  - create an order for a listing (price and seller come from the listing,
 *                                          never from the browser)
 *   2. POST /api/payments/create/{id}    - start the payment (Razorpay order, or a demo order when no keys are set)
 *   3. POST /api/payments/verify         - Razorpay success callback, verified server-side
 *      POST /api/payments/demo-confirm   - demo-mode equivalent
 * All of these are BUYER-only (see AccessRules).
 */
@RestController
@RequestMapping("/api")
public class PaymentController {

    private final OrderRepository orderRepository;
    private final ProduceListingRepository listingRepository;
    private final PaymentService paymentService;
    private final OrderViewService orderViews;

    public PaymentController(OrderRepository orderRepository, ProduceListingRepository listingRepository,
                             PaymentService paymentService, OrderViewService orderViews) {
        this.orderRepository = orderRepository;
        this.listingRepository = listingRepository;
        this.paymentService = paymentService;
        this.orderViews = orderViews;
    }

    @Data
    public static class CreateOrderRequest {
        private Long listingId;
        private Double quantityKg;
    }

    @Data
    public static class VerifyRequest {
        private String razorpayOrderId;
        private String razorpayPaymentId;
        private String razorpaySignature;
    }

    @Data
    public static class DemoPayRequest {
        private Long orderId;
        private String method; // UPI, CARD, NETBANKING
    }

    @PostMapping("/orders")
    public BuyerOrderView createOrder(@RequestBody CreateOrderRequest req, HttpServletRequest http) {
        User buyer = AuthInterceptor.currentUser(http);
        if (req.getListingId() == null) throw ApiException.badRequest("Please choose a product.");
        if (req.getQuantityKg() == null || req.getQuantityKg() <= 0) throw ApiException.badRequest("Please enter how many kg you want.");

        ProduceListing listing = listingRepository.findById(req.getListingId())
                .orElseThrow(() -> ApiException.notFound("That product is no longer listed."));
        if (!"AVAILABLE".equals(listing.getStatus()) || listing.getSellerUserId() == null) {
            throw ApiException.conflict("That product is not available right now.");
        }
        double available = listing.getQuantityKg() == null ? 0 : listing.getQuantityKg();
        if (req.getQuantityKg() > available) {
            throw ApiException.conflict("Only " + available + " kg is available.");
        }

        Order order = new Order();
        order.setListingId(listing.getId());
        order.setBuyerUserId(buyer.getId());
        order.setSellerUserId(listing.getSellerUserId());
        order.setCropName(listing.getCropName());
        order.setQuantityKg(req.getQuantityKg());
        order.setPricePerKg(listing.getExpectedPricePerKg());
        order.setTotalAmountRupees(Math.round(req.getQuantityKg() * listing.getExpectedPricePerKg() * 100.0) / 100.0);
        order.setStatus(Order.Status.PENDING_PAYMENT);
        order.setCreatedAt(LocalDateTime.now());
        order = orderRepository.save(order);
        return orderViews.buyerOrders(List.of(order)).get(0);
    }

    /** Public: tells the checkout page whether to use the real Razorpay widget or the demo checkout. */
    @GetMapping("/payments/config")
    public Map<String, Object> config() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("demoMode", paymentService.isDemoMode());
        m.put("razorpayKeyId", paymentService.getKeyId());
        return m;
    }

    @PostMapping("/payments/create/{orderId}")
    public Map<String, Object> createPayment(@PathVariable Long orderId, HttpServletRequest http) {
        Payment p = paymentService.createPaymentOrder(orderId, AuthInterceptor.currentUser(http));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("paymentId", p.getId());
        m.put("gatewayOrderId", p.getGatewayOrderId());
        m.put("amountRupees", p.getAmountRupees());
        m.put("demoMode", paymentService.isDemoMode());
        m.put("razorpayKeyId", paymentService.getKeyId());
        return m;
    }

    @PostMapping("/payments/verify")
    public Map<String, Object> verify(@RequestBody VerifyRequest req, HttpServletRequest http) {
        boolean ok = paymentService.verifyAndMarkPaid(req.getRazorpayOrderId(), req.getRazorpayPaymentId(),
                req.getRazorpaySignature(), AuthInterceptor.currentUser(http));
        return Map.of("verified", ok);
    }

    @PostMapping("/payments/demo-confirm")
    public Map<String, Object> demoConfirm(@RequestBody DemoPayRequest req, HttpServletRequest http) {
        Payment p = paymentService.confirmDemoPayment(req.getOrderId(), req.getMethod(), AuthInterceptor.currentUser(http));
        return Map.of("verified", true, "method", p.getMethod(), "demo", true);
    }
}
