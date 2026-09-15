package com.friendinneed.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class WeatherService {
    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);
    private final RestClient client = RestClient.create("https://api.open-meteo.com");

    public static String condition(int code) {
        return switch (code) {
            case 0 -> "clear";
            case 1, 2, 3 -> "partly cloudy";
            case 45, 48 -> "foggy";
            case 51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> "rainy";
            case 71, 73, 75, 77, 85, 86 -> "snowy";
            case 95, 96, 99 -> "stormy";
            default -> "mixed conditions";
        };
    }

    public String current(String location) {
        try {
            JsonNode places = client.get().uri("https://geocoding-api.open-meteo.com/v1/search?name={city}&count=1", location).retrieve().body(JsonNode.class);
            JsonNode place = places.path("results").path(0);
            if (place.isMissingNode()) return "Weather is unavailable for " + location + ".";
            JsonNode weather = client.get().uri("/v1/forecast?latitude={lat}&longitude={lon}&current=temperature_2m,apparent_temperature,weather_code", place.path("latitude").asDouble(), place.path("longitude").asDouble()).retrieve().body(JsonNode.class);
            JsonNode now = weather.path("current");
            return "Weather in " + place.path("name").asText(location) + ": " + now.path("temperature_2m").asDouble() + "°C (feels " + now.path("apparent_temperature").asDouble() + "°C), " + condition(now.path("weather_code").asInt()) + ".";
        } catch (Exception e) {
            log.warn("Weather API call failed", e);
            return "Live weather is temporarily unavailable.";
        }
    }
}
