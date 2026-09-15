package com.friendinneed.vision;

import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class VisionService {
    private final boolean enabled;
    private final String model, key;
    private final RestClient client;

    public VisionService(@Value("${vision.enabled:false}") boolean enabled, @Value("${vision.model:openai/gpt-4o}") String model, @Value("${OPENROUTER_BASE_URL:https://openrouter.ai/api}") String url, @Value("${OPENROUTER_API_KEY:}") String key) {
        this.enabled = enabled;
        this.model = model;
        this.key = key;
        client = RestClient.builder().baseUrl(url).build();
    }

    public String describeImage(byte[] image, String prompt) {
        if (!enabled || key.isBlank()) return "Vision is not enabled.";
        String data = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(image);
        var body = java.util.Map.of("model", model, "messages", java.util.List.of(java.util.Map.of("role", "user", "content", java.util.List.of(java.util.Map.of("type", "text", "text", prompt), java.util.Map.of("type", "image_url", "image_url", java.util.Map.of("url", data))))));
        try {
            return client.post().uri("/v1/chat/completions").header("Authorization", "Bearer " + key).body(body).retrieve().body(com.fasterxml.jackson.databind.JsonNode.class).path("choices").path(0).path("message").path("content").asText();
        } catch (Exception e) {
            return "I couldn't describe that image right now.";
        }
    }
}
