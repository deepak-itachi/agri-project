package com.agri.backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** One row per sign-in / registration / failed attempt - this is what the admin "Login activity" screen reads. */
@Entity
@Table(name = "login_events")
@Data
public class LoginEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String email;
    private String fullName;
    private String role;        // FARMER, SELLER, BUYER, ADMIN
    private String eventType;   // LOGIN, REGISTER, LOGOUT, LOGIN_FAILED
    private String ipAddress;

    @Column(length = 300)
    private String userAgent;

    private LocalDateTime createdAt = LocalDateTime.now();
}
