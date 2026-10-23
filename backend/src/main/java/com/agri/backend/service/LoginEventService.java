package com.agri.backend.service;

import com.agri.backend.model.LoginEvent;
import com.agri.backend.model.User;
import com.agri.backend.repository.LoginEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** Writes the audit trail the admin sees under "Login activity". */
@Service
public class LoginEventService {

    private final LoginEventRepository repository;

    public LoginEventService(LoginEventRepository repository) {
        this.repository = repository;
    }

    public void record(User user, String email, String role, String type, HttpServletRequest request) {
        LoginEvent e = new LoginEvent();
        if (user != null) {
            e.setUserId(user.getId());
            e.setEmail(user.getEmail());
            e.setFullName(user.getFullName());
            e.setRole(user.getRole() == null ? role : user.getRole().name());
        } else {
            e.setEmail(email);
            e.setRole(role);
        }
        e.setEventType(type);
        e.setCreatedAt(LocalDateTime.now());
        if (request != null) {
            String forwarded = request.getHeader("X-Forwarded-For");
            e.setIpAddress(forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : request.getRemoteAddr());
            String ua = request.getHeader("User-Agent");
            e.setUserAgent(ua == null ? null : (ua.length() > 290 ? ua.substring(0, 290) : ua));
        }
        repository.save(e);
    }
}
