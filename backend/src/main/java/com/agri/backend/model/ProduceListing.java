package com.agri.backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A product a seller has put on the marketplace. Buyers never receive this entity directly -
 * they get ApiDtos.ListingView, which carries only the shop name (seller privacy).
 */
@Entity
@Table(name = "produce_listings")
@Data
public class ProduceListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owner (a SELLER user). */
    private Long sellerUserId;

    private String cropName;
    private Double quantityKg;
    private Double expectedPricePerKg;
    private String location;
    private LocalDate availableFrom;

    @Column(length = 1000)
    private String description;

    /** Photo uploaded by the seller, e.g. /uploads/listings/abc.jpg. Empty -> crop dataset image is used. */
    @Column(length = 500)
    private String imageUrl;

    private String status; // AVAILABLE, SOLD_OUT, HIDDEN
    private LocalDateTime createdAt = LocalDateTime.now();
}
