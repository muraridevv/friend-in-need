package com.friendinneed.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Calls the dedicated embedding model, independently of the conversation model.
 */
@Service
public class EmbeddingService {
    private final EmbeddingProperties properties;
    private final RestClient client;

    public EmbeddingService(EmbeddingProperties properties) {
        this.properties = properties;
        String baseUrl = properties.baseUrl().endsWith("/") ? properties.baseUrl() : properties.baseUrl() + "/";
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    public float[] embed(String text) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) return new float[0];
        JsonNode response = client.post().uri("v1/embeddings").header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .body(Map.of("model", properties.model(), "input", text)).retrieve().body(JsonNode.class);
        if (response == null) return new float[0];
        JsonNode values = response.path("data").path(0).path("embedding");
        float[] result = new float[values.size()];
        for (int index = 0; index < values.size(); index++) result[index] = (float) values.get(index).asDouble();
        return result;
    }

    public String model() {
        return properties.model();
    }

    public static String serialize(float[] values) {
        return java.util.Arrays.toString(values);
    }

    public static float[] deserialize(String raw) {
        if (raw == null || raw.length() < 3) return new float[0];
        String[] parts = raw.substring(1, raw.length() - 1).split(",");
        float[] values = new float[parts.length];
        for (int index = 0; index < parts.length; index++) values[index] = Float.parseFloat(parts[index].trim());
        return values;
    }

}
