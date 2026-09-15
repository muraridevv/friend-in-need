package com.friendinneed.embedding;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("embedding")
public record EmbeddingProperties(String baseUrl, String apiKey, String model) {
}
