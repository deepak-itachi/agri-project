package com.agri.backend.controller;

import com.agri.backend.dto.ApiDtos.ListingView;
import com.agri.backend.exception.ApiException;
import com.agri.backend.model.ProduceListing;
import com.agri.backend.repository.ProduceListingRepository;
import com.agri.backend.service.ListingService;
import com.agri.backend.service.MarketPriceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Public marketplace browsing (what buyers see) + mandi price lookup. Selling happens under /api/seller. */
@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final MarketPriceService marketPriceService;
    private final ProduceListingRepository produceListingRepository;
    private final ListingService listingService;

    public MarketController(MarketPriceService marketPriceService,
                            ProduceListingRepository produceListingRepository,
                            ListingService listingService) {
        this.marketPriceService = marketPriceService;
        this.produceListingRepository = produceListingRepository;
        this.listingService = listingService;
    }

    @GetMapping("/mandi-prices")
    public Map<String, Object> mandiPrices(@RequestParam String state, @RequestParam String commodity) {
        return marketPriceService.getMandiPrices(state, commodity);
    }

    /** Available produce, newest first. Optional free-text filter q matches crop name or location. */
    @GetMapping("/listings")
    public List<ListingView> listings(@RequestParam(required = false) String q) {
        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        List<ProduceListing> rows = produceListingRepository.findByStatusOrderByCreatedAtDesc("AVAILABLE").stream()
                .filter(l -> l.getQuantityKg() != null && l.getQuantityKg() > 0)
                .filter(l -> needle.isEmpty()
                        || (l.getCropName() != null && l.getCropName().toLowerCase(Locale.ROOT).contains(needle))
                        || (l.getLocation() != null && l.getLocation().toLowerCase(Locale.ROOT).contains(needle)))
                .toList();
        return listingService.toBuyerViews(rows);
    }

    @GetMapping("/listings/{id}")
    public ListingView listing(@PathVariable Long id) {
        ProduceListing l = produceListingRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("That product is no longer listed."));
        if (!"AVAILABLE".equals(l.getStatus())) throw ApiException.notFound("That product is no longer available.");
        return listingService.toBuyerView(l);
    }
}
