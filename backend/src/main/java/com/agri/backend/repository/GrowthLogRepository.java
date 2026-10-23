package com.agri.backend.repository;

import com.agri.backend.model.GrowthLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrowthLogRepository extends JpaRepository<GrowthLog, Long> {
    List<GrowthLog> findByFarmerId(Long farmerId);
}
