package com.agri.backend.repository;

import com.agri.backend.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByBuyerUserId(Long buyerUserId);
    List<Order> findBySellerUserId(Long sellerUserId);
    List<Order> findByBuyerUserIdOrderByCreatedAtDesc(Long buyerUserId);
    List<Order> findBySellerUserIdOrderByCreatedAtDesc(Long sellerUserId);
    List<Order> findAllByOrderByCreatedAtDesc();
    boolean existsByListingId(Long listingId);
}
