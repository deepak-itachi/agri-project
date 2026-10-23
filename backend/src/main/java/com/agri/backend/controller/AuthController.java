package com.agri.backend.controller;

import com.agri.backend.dto.ApiDtos.UserView;
import com.agri.backend.exception.ApiException;
import com.agri.backend.model.User;
import com.agri.backend.repository.UserRepository;
import com.agri.backend.security.AuthInterceptor;
import com.agri.backend.security.JwtService;
import com.agri.backend.security.LoginThrottle;
import com.agri.backend.service.LoginEventService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Separate sign-up/sign-in per account type (learner/seller/buyer; admin accounts are created by the server, never
 * via sign-up). A successful call returns {"token": "<jwt>", "user": {...}}; the frontend sends the token as
 * "Authorization: Bearer <token>" on every later request.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern UPI = Pattern.compile("^[\\w.\\-]{2,}@[A-Za-z]{2,}$");

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final LoginEventService loginEvents;
    private final LoginThrottle throttle;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthController(UserRepository userRepository, JwtService jwtService,
                          LoginEventService loginEvents, LoginThrottle throttle) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.loginEvents = loginEvents;
        this.throttle = throttle;
    }

    @Data
    public static class RegisterRequest {
        private String email;
        private String password;
        private String fullName;
        private String phone;
        private String city;
        private String businessName;       // sellers: public shop name
        private String upiId;              // sellers: payout destination
        private String bankAccountNumber;  // sellers: alternative to UPI
        private String bankIfsc;
    }

    @Data
    public static class LoginRequest {
        private String email;
        private String password;
        private String role; // FARMER, SELLER, BUYER, ADMIN
    }

    @Data
    public static class ProfileRequest {
        private String fullName;
        private String phone;
        private String city;
        private String businessName;
        private String upiId;
        private String bankAccountNumber;
        private String bankIfsc;
    }

    // ---------------------------------------------------------------- register

    @PostMapping("/register/farmer")
    public Map<String, Object> registerFarmer(@RequestBody RegisterRequest req, HttpServletRequest http) {
        return register(req, User.Role.FARMER, http);
    }

    @PostMapping("/register/seller")
    public Map<String, Object> registerSeller(@RequestBody RegisterRequest req, HttpServletRequest http) {
        return register(req, User.Role.SELLER, http);
    }

    @PostMapping("/register/buyer")
    public Map<String, Object> registerBuyer(@RequestBody RegisterRequest req, HttpServletRequest http) {
        return register(req, User.Role.BUYER, http);
    }

    private Map<String, Object> register(RegisterRequest req, User.Role role, HttpServletRequest http) {
        String email = normalizeEmail(req.getEmail());
        if (req.getPassword() == null || req.getPassword().length() < 8) {
            throw ApiException.badRequest("Password must be at least 8 characters long.");
        }
        if (isBlank(req.getFullName())) {
            throw ApiException.badRequest("Please enter your full name.");
        }
        if (userRepository.existsByEmailAndRole(email, role)) {
            throw ApiException.conflict("An account with this email already exists for this account type. Please sign in instead.");
        }
        if (role == User.Role.SELLER) {
            boolean hasUpi = !isBlank(req.getUpiId());
            boolean hasBank = !isBlank(req.getBankAccountNumber()) && !isBlank(req.getBankIfsc());
            if (!hasUpi && !hasBank) {
                throw ApiException.badRequest("Sellers must provide a UPI ID, or a bank account number with IFSC, so we can pay you.");
            }
            if (hasUpi && !UPI.matcher(req.getUpiId().trim()).matches()) {
                throw ApiException.badRequest("That UPI ID doesn't look right. It should look like name@bank.");
            }
        }

        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(req.getPassword()));
        u.setFullName(req.getFullName().trim());
        u.setPhone(trimToNull(req.getPhone()));
        u.setCity(trimToNull(req.getCity()));
        u.setRole(role);
        u.setActive(true);
        u.setCreatedAt(LocalDateTime.now());
        u.setLastLoginAt(LocalDateTime.now());
        u.setLoginCount(1);
        if (role == User.Role.SELLER) {
            u.setBusinessName(isBlank(req.getBusinessName()) ? u.getFullName() : req.getBusinessName().trim());
            u.setUpiId(trimToNull(req.getUpiId()));
            u.setBankAccountNumber(trimToNull(req.getBankAccountNumber()));
            u.setBankIfsc(trimToNull(req.getBankIfsc()));
        }
        u = userRepository.save(u);
        loginEvents.record(u, email, role.name(), "REGISTER", http);
        return session(u);
    }

    // ------------------------------------------------------------------- login

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest req, HttpServletRequest http) {
        User.Role role;
        try {
            role = User.Role.valueOf(req.getRole() == null ? "" : req.getRole().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Please choose how you want to sign in (learner, seller, buyer or admin).");
        }
        String email = normalizeEmail(req.getEmail());
        String key = role + ":" + email;
        if (throttle.isLocked(key)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed attempts. Please wait about 10 minutes and try again.");
        }

        Optional<User> found = userRepository.findByEmailAndRole(email, role);
        if (found.isEmpty() || req.getPassword() == null
                || !encoder.matches(req.getPassword(), found.get().getPasswordHash())) {
            throttle.recordFailure(key);
            loginEvents.record(found.orElse(null), email, role.name(), "LOGIN_FAILED", http);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Incorrect email or password for a "
                    + role.name().toLowerCase(Locale.ROOT).replace("farmer", "learner") + " account.");
        }
        User u = found.get();
        if (!u.isEnabled()) {
            throw ApiException.forbidden("This account has been disabled. Please contact the administrator.");
        }
        throttle.clear(key);
        u.setLastLoginAt(LocalDateTime.now());
        u.setLoginCount((u.getLoginCount() == null ? 0 : u.getLoginCount()) + 1);
        userRepository.save(u);
        loginEvents.record(u, email, role.name(), "LOGIN", http);
        return session(u);
    }

    // ----------------------------------------------------------- current user

    @GetMapping("/me")
    public UserView me(HttpServletRequest http) {
        return UserView.of(AuthInterceptor.currentUser(http));
    }

    @PutMapping("/me")
    public UserView updateMe(@RequestBody ProfileRequest req, HttpServletRequest http) {
        User u = AuthInterceptor.currentUser(http);
        if (!isBlank(req.getFullName())) u.setFullName(req.getFullName().trim());
        u.setPhone(trimToNull(req.getPhone()));
        u.setCity(trimToNull(req.getCity()));
        if (u.getRole() == User.Role.SELLER) {
            if (!isBlank(req.getBusinessName())) u.setBusinessName(req.getBusinessName().trim());
            if (!isBlank(req.getUpiId()) && !UPI.matcher(req.getUpiId().trim()).matches()) {
                throw ApiException.badRequest("That UPI ID doesn't look right. It should look like name@bank.");
            }
            u.setUpiId(trimToNull(req.getUpiId()));
            u.setBankAccountNumber(trimToNull(req.getBankAccountNumber()));
            u.setBankIfsc(trimToNull(req.getBankIfsc()));
        }
        return UserView.of(userRepository.save(u));
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest http) {
        User u = AuthInterceptor.currentUser(http);
        loginEvents.record(u, u.getEmail(), u.getRole().name(), "LOGOUT", http);
        return Map.of("ok", true);
    }

    // ----------------------------------------------------------------- helpers

    private Map<String, Object> session(User u) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", jwtService.createToken(u.getId(), u.getRole().name()));
        body.put("user", UserView.of(u));
        return body;
    }

    private static String normalizeEmail(String raw) {
        String email = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(email).matches()) throw ApiException.badRequest("Please enter a valid email address.");
        return email;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String trimToNull(String s) {
        return isBlank(s) ? null : s.trim();
    }
}
