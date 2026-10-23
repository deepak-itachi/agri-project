package com.agri.backend.repository;

import com.agri.backend.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);
    /** Latest payment attempt for an order (an order can have several attempts if one failed). */
    Optional<Payment> findFirstByOrderIdOrderByIdDesc(Long orderId);
}
