package com.agri.backend.controller;

import com.agri.backend.service.WeatherService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/current")
    public Map<String, Object> current(@RequestParam String location) {
        return weatherService.getCurrentWeather(location);
    }

    @GetMapping("/forecast")
    public Map<String, Object> forecast(@RequestParam String location) {
        return weatherService.getForecast(location);
    }
}
