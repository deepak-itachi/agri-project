package com.agri.backend.repository;

import com.agri.backend.model.LandPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LandPhotoRepository extends JpaRepository<LandPhoto, Long> {
    List<LandPhoto> findByFarmerIdOrderByUploadedAtDesc(Long farmerId);
}
