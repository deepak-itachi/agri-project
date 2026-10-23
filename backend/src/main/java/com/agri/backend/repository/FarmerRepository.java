package com.agri.backend.repository;

import com.agri.backend.model.Farmer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmerRepository extends JpaRepository<Farmer, Long> {
}
