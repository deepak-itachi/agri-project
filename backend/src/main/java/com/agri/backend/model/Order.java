package com.agri.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/** A buyer's order against one marketplace listing. One order -> one payment. */
@Entity
@Table(name = "orders")
@Data
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long listingId;
    private Long buyerUserId;
    private Long sellerUserId;

    private String cropName;
    private Double quantityKg;
    private Double pricePerKg;
    private Double totalAmountRupees;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING_PAYMENT;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime paidAt;

    public enum Status { PENDING_PAYMENT, PAID, SHIPPED, DELIVERED, CANCELLED }
}
