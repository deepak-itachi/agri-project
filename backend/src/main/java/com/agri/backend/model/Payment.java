package com.agri.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * A payment attempt/record for an Order, made through the payment gateway
 * (Razorpay - see PaymentService for why). Covers both card and UPI (which
 * is how a buyer pays via GPay/PhonePe/any UPI app - see notes in
 * PaymentController).
 */
@Entity
@Table(name = "payments")
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    private String gatewayOrderId;     // Razorpay order id
    private String gatewayPaymentId;   // filled in after success
    private String method;             // CARD, UPI, NETBANKING, WALLET (from gateway webhook)
    private Double amountRupees;

    @Enumerated(EnumType.STRING)
    private Status status = Status.CREATED;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime settledAt;

    public enum Status { CREATED, SUCCESS, FAILED, REFUNDED }
}
