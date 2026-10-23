package com.agri.backend.repository;

import com.agri.backend.model.SoilReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SoilReportRepository extends JpaRepository<SoilReport, Long> {
    List<SoilReport> findByFarmerIdOrderByUploadedAtDesc(Long farmerId);
}
