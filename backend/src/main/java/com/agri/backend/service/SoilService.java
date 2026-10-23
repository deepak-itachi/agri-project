package com.agri.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * External API #2: ISRIC SoilGrids (free, no key needed)
 * https://rest.isric.org/soilgrids/v2.0/docs
 * Used for: soil property lookup (pH, organic carbon, sand/silt/clay %) by lat/long.
 *
 * For terrace/container farmers with no natural soil to test, the frontend
 * skips this call and instead shows the static "potting mix guide" content.
 */
@Service
public class SoilService {

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> getSoilProperties(double lat, double lon) {
        String url = String.format(
                "https://rest.isric.org/soilgrids/v2.0/properties/query?lon=%f&lat=%f&property=phh2o&property=soc&property=clay&property=sand&depth=0-5cm&value=mean",
                lon, lat);
        return restTemplate.getForObject(url, Map.class);
    }
}
