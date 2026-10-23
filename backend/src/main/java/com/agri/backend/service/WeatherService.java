package com.agri.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * External API #1: OpenWeatherMap
 * Used for: irrigation timing, sowing-window suggestions, weather alerts.
 * Sign up for a free key at https://openweathermap.org/api
 */
@Service
public class WeatherService {

    @Value("${external.openweather.apikey:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> getCurrentWeather(String cityOrLocation) {
        String url = String.format(
                "https://api.openweathermap.org/data/2.5/weather?q=%s&units=metric&appid=%s",
                cityOrLocation, apiKey);
        return restTemplate.getForObject(url, Map.class);
    }

    public Map<String, Object> getForecast(String cityOrLocation) {
        String url = String.format(
                "https://api.openweathermap.org/data/2.5/forecast?q=%s&units=metric&appid=%s",
                cityOrLocation, apiKey);
        return restTemplate.getForObject(url, Map.class);
    }
}
