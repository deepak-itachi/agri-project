package com.agri.backend.repository;

import com.agri.backend.model.WaterResource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WaterResourceRepository extends JpaRepository<WaterResource, Long> {
    List<WaterResource> findByFarmerId(Long farmerId);
}
