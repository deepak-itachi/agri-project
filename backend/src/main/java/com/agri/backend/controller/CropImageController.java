package com.agri.backend.controller;

import com.agri.backend.model.CropImage;
import com.agri.backend.repository.CropImageRepository;
import com.agri.backend.service.ListingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Representative photo for a crop (the crop-image dataset). Admins edit this data under /api/admin/crop-images.
 */
@RestController
@RequestMapping("/api/crop-images")
public class CropImageController {

    private final CropImageRepository cropImageRepository;

    public CropImageController(CropImageRepository cropImageRepository) {
        this.cropImageRepository = cropImageRepository;
    }

    @GetMapping
    public List<CropImage> all() {
        return cropImageRepository.findAll();
    }

    @GetMapping("/{cropName}")
    public CropImage getByCropName(@PathVariable String cropName) {
        return cropImageRepository.findFirstByCropNameIgnoreCase(cropName).orElseGet(() -> {
            CropImage fallback = new CropImage();
            fallback.setCropName(cropName);
            fallback.setImageUrl(ListingService.DEFAULT_IMAGE);
            return fallback;
        });
    }
}
