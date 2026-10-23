package com.agri.backend.soap;

import com.agri.backend.soap.gen.FarmerRequirementRequest;
import com.agri.backend.soap.gen.FarmerRequirementResponse;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

/**
 * SOAP Web Service: Farmer Requirement Analyzer
 * ------------------------------------------------
 * This is the required SOAP component of the project.
 * WSDL will be auto-published at: /ws/farmerRequirement.wsdl
 *
 * It classifies an incoming farmer as small/medium/large scale and
 * open-field/terrace/container based, and gives a starting recommendation.
 * The result feeds into the crop recommendation + profit calculator modules.
 */
@Endpoint
public class FarmerRequirementEndpoint {

    private static final String NAMESPACE_URI = "http://agri.com/soap/farmer";

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "farmerRequirementRequest")
    @ResponsePayload
    public FarmerRequirementResponse analyze(@RequestPayload FarmerRequirementRequest request) {
        FarmerRequirementResponse response = new FarmerRequirementResponse();

        double land = request.getLandSizeSqft();
        String landType = request.getLandType().name();
        String experience = request.getExperienceLevel().name();

        String scale;
        String method;
        String note;

        if (landType.equals("TERRACE") || landType.equals("ROOFTOP_CONTAINER")) {
            scale = "SMALL_SCALE_URBAN";
            method = "Container / grow-bag farming";
            note = "Great starting point for urban beginners - focus on leafy greens, herbs, and short-duration vegetables. Low investment, quick returns.";
        } else if (landType.equals("BACKYARD") || land <= 5000) {
            scale = "SMALL_SCALE";
            method = "Raised-bed or kitchen garden farming";
            note = "Suitable for beginners wanting a small but real farming setup. Good for vegetables and short-cycle crops.";
        } else if (land <= 43560) { // up to ~1 acre
            scale = "MEDIUM_SCALE";
            method = "Open-field row cropping";
            note = "Enough land for a semi-commercial crop cycle. Consider vegetables, pulses, or a single cash crop to start.";
        } else {
            scale = "LARGE_SCALE";
            method = "Open-field mechanized / cash-crop farming";
            note = "Land size supports commercial cultivation. Consider cereals, cash crops, or contract farming tie-ups.";
        }

        if (experience.equals("BEGINNER")) {
            note += " As a beginner, start with one crop cycle before scaling up.";
        }

        response.setFarmerCategory(experience + "_" + landType);
        response.setScale(scale);
        response.setSuggestedFarmingMethod(method);
        response.setRecommendationNote(note);

        return response;
    }
}
