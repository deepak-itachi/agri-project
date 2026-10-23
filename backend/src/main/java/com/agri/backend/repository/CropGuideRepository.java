package com.agri.backend.repository;

import com.agri.backend.model.CropGuide;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CropGuideRepository extends JpaRepository<CropGuide, Long> {
    List<CropGuide> findBySuitableScale(String suitableScale);
    List<CropGuide> findBySuitableLandTypesContaining(String landType);
}
