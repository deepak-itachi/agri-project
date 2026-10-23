package com.agri.backend.repository;

import com.agri.backend.model.CropImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CropImageRepository extends JpaRepository<CropImage, Long> {
    Optional<CropImage> findFirstByCropNameIgnoreCase(String cropName);
}
