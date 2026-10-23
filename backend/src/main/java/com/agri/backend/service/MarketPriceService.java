package com.agri.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * External API #3: data.gov.in - "Current Daily Price of Various Commodities"
 * (Agmarknet mandi price dataset). Get a free API key at https://data.gov.in/
 * Used for: profit prediction + wholesale buyer / mandi market finder.
 */
@Service
public class MarketPriceService {

    @Value("${external.datagovin.apikey:}")
    private String apiKey;

    private static final String RESOURCE_ID = "9ef84268-d588-465a-a308-a864a43d0070";

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> getMandiPrices(String state, String commodity) {
        String url = String.format(
                "https://api.data.gov.in/resource/%s?api-key=%s&format=json&filters[state]=%s&filters[commodity]=%s&limit=20",
                RESOURCE_ID, apiKey, state, commodity);
        return restTemplate.getForObject(url, Map.class);
    }
}
