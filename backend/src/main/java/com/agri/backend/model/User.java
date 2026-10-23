package com.agri.backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * One account table, four roles:
 *  - ADMIN  : platform owner - sees every user, login activity, orders and can edit all data
 *  - FARMER : learner / trainee farmer (advisory flow: onboarding, crop guides, growth tracking)
 *  - SELLER : lists produce for sale and is paid for it
 *  - BUYER  : trader who buys produce through the marketplace
 * Email + role together identify an account, so one person can hold e.g. a learner AND a seller account.
 */
@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String fullName;
    private String phone;
    private String city;

    /** Public shop name shown to buyers instead of the seller's personal details. */
    private String businessName;

    @Enumerated(EnumType.STRING)
    private Role role;

    // Payout destination for SELLER accounts - never exposed to buyers.
    private String upiId;
    private String bankAccountNumber;
    private String bankIfsc;

    /** Link to the detailed farm profile created during learner onboarding. */
    private Long farmerId;

    private Boolean active = true;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime lastLoginAt;
    private Integer loginCount = 0;

    public enum Role { ADMIN, FARMER, SELLER, BUYER }

    public boolean isEnabled() {
        return !Boolean.FALSE.equals(active);
    }
}
