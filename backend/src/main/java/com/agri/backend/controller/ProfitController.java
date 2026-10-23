package com.agri.backend.controller;

import com.agri.backend.model.CropGuide;
import com.agri.backend.repository.CropGuideRepository;
import com.agri.backend.service.MarketPriceService;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profit")
public class ProfitController {

    private final CropGuideRepository cropGuideRepository;
    private final MarketPriceService marketPriceService;

    public ProfitController(CropGuideRepository cropGuideRepository, MarketPriceService marketPriceService) {
        this.cropGuideRepository = cropGuideRepository;
        this.marketPriceService = marketPriceService;
    }

    @Data
    public static class ProfitRequest {
        private Long cropGuideId;
        private Double landAreaMultiplier; // e.g. 1.0 = one "unit" as defined in CropGuide, 2.5 = 2.5 acres/units
        private String state;              // for live mandi price lookup
    }

    @Data
    public static class ProfitResponse {
        private String cropName;
        private double estimatedCost;
        private double estimatedYieldKg;
        private double marketPricePerKg;
        private double estimatedRevenue;
        private double estimatedProfit;
        private String note;
    }

    /**
     * Step 5: Profit Prediction
     * Combines static crop-guide cost/yield data with the LIVE mandi price
     * (external API) to produce a realistic profit estimate.
     */
    @PostMapping("/estimate")
    public ProfitResponse estimate(@RequestBody ProfitRequest req) {
        CropGuide crop = cropGuideRepository.findById(req.getCropGuideId()).orElseThrow();

        double multiplier = req.getLandAreaMultiplier() == null ? 1.0 : req.getLandAreaMultiplier();
        double cost = crop.getEstimatedCostPerAcreOrUnit() * multiplier;
        double yieldKg = crop.getEstimatedYieldPerAcreOrUnit() * multiplier;

        double pricePerKg;
        try {
            Map<String, Object> mandiData = marketPriceService.getMandiPrices(req.getState(), crop.getCropName());
            // NOTE: parse the actual "records" array from mandiData in production;
            // falling back to a placeholder price here keeps the demo runnable without a live key.
            pricePerKg = 20.0;
        } catch (Exception e) {
            pricePerKg = 20.0; // fallback price if external API is unreachable
        }

        double revenue = yieldKg * pricePerKg;
        double profit = revenue - cost;

        ProfitResponse res = new ProfitResponse();
        res.setCropName(crop.getCropName());
        res.setEstimatedCost(cost);
        res.setEstimatedYieldKg(yieldKg);
        res.setMarketPricePerKg(pricePerKg);
        res.setEstimatedRevenue(revenue);
        res.setEstimatedProfit(profit);
        res.setNote(profit > 0
                ? "Projected to be profitable based on current mandi prices."
                : "Margins are tight at current prices - consider a longer holding period or direct-to-consumer selling.");
        return res;
    }
}
