package com.agri.backend.repository;

import com.agri.backend.model.ProduceListing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProduceListingRepository extends JpaRepository<ProduceListing, Long> {
    List<ProduceListing> findByStatus(String status);
    List<ProduceListing> findByStatusOrderByCreatedAtDesc(String status);
    List<ProduceListing> findByCropNameIgnoreCase(String cropName);
    List<ProduceListing> findBySellerUserIdOrderByCreatedAtDesc(Long sellerUserId);
    List<ProduceListing> findAllByOrderByCreatedAtDesc();
}
