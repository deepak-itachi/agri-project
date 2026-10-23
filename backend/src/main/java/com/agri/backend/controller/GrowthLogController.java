package com.agri.backend.controller;

import com.agri.backend.exception.ApiException;
import com.agri.backend.model.GrowthLog;
import com.agri.backend.model.User;
import com.agri.backend.repository.FarmerRepository;
import com.agri.backend.repository.GrowthLogRepository;
import com.agri.backend.security.AuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Step 4 tracking: a learner logs progress through their crop cycle. Learners can only touch their own logs. */
@RestController
@RequestMapping("/api/growth-logs")
public class GrowthLogController {

    private final GrowthLogRepository growthLogRepository;
    private final FarmerRepository farmerRepository;

    public GrowthLogController(GrowthLogRepository growthLogRepository, FarmerRepository farmerRepository) {
        this.growthLogRepository = growthLogRepository;
        this.farmerRepository = farmerRepository;
    }

    @GetMapping("/farmer/{farmerId}")
    public List<GrowthLog> byFarmer(@PathVariable Long farmerId) {
        return growthLogRepository.findByFarmerId(farmerId);
    }

    @PostMapping
    public GrowthLog create(@RequestBody GrowthLog log, HttpServletRequest http) {
        User user = AuthInterceptor.currentUser(http);
        if (user.getRole() == User.Role.FARMER) {
            if (user.getFarmerId() == null) throw ApiException.badRequest("Please complete your farm profile first.");
            log.setFarmer(farmerRepository.findById(user.getFarmerId()).orElseThrow());
        }
        return growthLogRepository.save(log);
    }

    @PutMapping("/{id}/stage")
    public GrowthLog updateStage(@PathVariable Long id, @RequestParam String stage, HttpServletRequest http) {
        User user = AuthInterceptor.currentUser(http);
        GrowthLog log = growthLogRepository.findById(id).orElseThrow();
        if (user.getRole() == User.Role.FARMER
                && (log.getFarmer() == null || !log.getFarmer().getId().equals(user.getFarmerId()))) {
            throw ApiException.forbidden("That growth log belongs to another farmer.");
        }
        log.setCurrentStage(stage);
        return growthLogRepository.save(log);
    }
}
