package com.agri.backend.controller;

import com.agri.backend.model.CropGuide;
import com.agri.backend.model.Farmer;
import com.agri.backend.repository.CropGuideRepository;
import com.agri.backend.repository.FarmerRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crops")
public class CropController {

    private final CropGuideRepository cropGuideRepository;
    private final FarmerRepository farmerRepository;

    public CropController(CropGuideRepository cropGuideRepository, FarmerRepository farmerRepository) {
        this.cropGuideRepository = cropGuideRepository;
        this.farmerRepository = farmerRepository;
    }

    /** Step 3: recommend crops suited to this farmer's scale + land type */
    @GetMapping("/recommend/{farmerId}")
    public List<CropGuide> recommend(@PathVariable Long farmerId) {
        Farmer farmer = farmerRepository.findById(farmerId).orElseThrow();
        List<CropGuide> byScale = cropGuideRepository.findBySuitableScale(farmer.getScale());
        // further filter in-memory by land type match
        return byScale.stream()
                .filter(c -> c.getSuitableLandTypes().contains(farmer.getLandType()))
                .toList();
    }

    /** Step 4: full stage-wise growth guide for a chosen crop */
    @GetMapping("/{id}/guide")
    public CropGuide getGuide(@PathVariable Long id) {
        return cropGuideRepository.findById(id).orElseThrow();
    }

    @GetMapping
    public List<CropGuide> all() {
        return cropGuideRepository.findAll();
    }

    @PostMapping
    public CropGuide create(@RequestBody CropGuide guide) {
        return cropGuideRepository.save(guide);
    }
}
