package com.agri.backend.controller;

import com.agri.backend.model.Farmer;
import com.agri.backend.model.User;
import com.agri.backend.repository.FarmerRepository;
import com.agri.backend.repository.UserRepository;
import com.agri.backend.security.AuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import com.agri.backend.soap.FarmerRequirementClient;
import com.agri.backend.soap.gen.ExperienceLevel;
import com.agri.backend.soap.gen.FarmerRequirementRequest;
import com.agri.backend.soap.gen.FarmerRequirementResponse;
import com.agri.backend.soap.gen.LandType;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/farmers")
public class FarmerController {

    private final FarmerRequirementClient soapClient;
    private final FarmerRepository farmerRepository;
    private final UserRepository userRepository;

    public FarmerController(FarmerRequirementClient soapClient, FarmerRepository farmerRepository,
                            UserRepository userRepository) {
        this.soapClient = soapClient;
        this.farmerRepository = farmerRepository;
        this.userRepository = userRepository;
    }

    @Data
    public static class OnboardingRequest {
        private String name;
        private String email;
        private String location;
        private Double landSizeSqft;
        private String landType;        // OPEN_FIELD, TERRACE, BACKYARD, ROOFTOP_CONTAINER
        private String experienceLevel; // BEGINNER, HOBBYIST, SERIOUS
        private Double budgetInRupees;
    }

    /**
     * Step 1 of the user journey: farmer submits their basic details.
     * This REST endpoint calls the internal SOAP service to classify them,
     * then persists the profile + classification to the database.
     */
    @PostMapping("/onboard")
    public Farmer onboard(@RequestBody OnboardingRequest req, HttpServletRequest http) {
        FarmerRequirementRequest soapRequest = new FarmerRequirementRequest();
        soapRequest.setLandSizeSqft(req.getLandSizeSqft());
        soapRequest.setLandType(LandType.valueOf(req.getLandType()));
        soapRequest.setLocation(req.getLocation());
        soapRequest.setBudgetInRupees(req.getBudgetInRupees());
        soapRequest.setExperienceLevel(ExperienceLevel.valueOf(req.getExperienceLevel()));

        FarmerRequirementResponse soapResponse = soapClient.analyze(soapRequest);

        Farmer farmer = new Farmer();
        farmer.setName(req.getName());
        farmer.setEmail(req.getEmail());
        farmer.setLocation(req.getLocation());
        farmer.setLandSizeSqft(req.getLandSizeSqft());
        farmer.setLandType(req.getLandType());
        farmer.setExperienceLevel(req.getExperienceLevel());
        farmer.setBudgetInRupees(req.getBudgetInRupees());
        farmer.setScale(soapResponse.getScale());
        farmer.setSuggestedFarmingMethod(soapResponse.getSuggestedFarmingMethod());

        Farmer saved = farmerRepository.save(farmer);

        // Link the signed-in learner account to this farm profile so the dashboard can find it next time.
        User account = AuthInterceptor.currentUser(http);
        if (account != null && account.getRole() == User.Role.FARMER) {
            account.setFarmerId(saved.getId());
            userRepository.save(account);
        }
        return saved;
    }

    @GetMapping("/{id}")
    public Farmer getFarmer(@PathVariable Long id) {
        return farmerRepository.findById(id).orElseThrow();
    }
}
