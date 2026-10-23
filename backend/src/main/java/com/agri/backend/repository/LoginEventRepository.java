package com.agri.backend.repository;

import com.agri.backend.model.LoginEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LoginEventRepository extends JpaRepository<LoginEvent, Long> {
    List<LoginEvent> findTop300ByOrderByCreatedAtDesc();
    List<LoginEvent> findTop300ByRoleOrderByCreatedAtDesc(String role);
    List<LoginEvent> findByEventTypeAndCreatedAtAfter(String eventType, LocalDateTime after);
    long countByEventTypeAndCreatedAtAfter(String eventType, LocalDateTime after);
}
