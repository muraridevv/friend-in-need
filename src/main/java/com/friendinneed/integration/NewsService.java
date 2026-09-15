package com.friendinneed.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.friendinneed.consent.ConsentService;
import com.friendinneed.consent.IntegrationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);

    private final ConsentService consents;
    private final RestClient client;
    private final String key;

    public NewsService(
            ConsentService consents,
            RestClient.Builder builder,
            @Value("${news.base-url:https://newsapi.org/v2}") String baseUrl,
            @Value("${news.api-key:}") String key) {
        this.consents = consents;
        this.key = key;

        RestClient.Builder clientBuilder = builder.baseUrl(baseUrl);
        if (key != null && !key.isBlank()) {
            clientBuilder.defaultHeader("X-Api-Key", key);
        }
        this.client = clientBuilder.build();
    }

    public List<NewsItem> headlines(UUID profileId, String category, int count) {
        if (!consents.isEnabled(profileId, IntegrationType.NEWS)) {
            return List.of();
        }
        return headlines(category, count);
    }

    public List<NewsItem> headlines(String category, int count) {
        if (key == null || key.isBlank()) {
            log.warn("NewsAPI key is not configured; skipping headline fetch.");
            return List.of();
        }

        int pageSize = Math.clamp(count, 1, 100);

        try {
            JsonNode response = client.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/top-headlines")
                                .queryParam("pageSize", pageSize);

                        // NewsAPI requires country or category. Default to country=us if category is omitted.
                        if (category != null && !category.isBlank()) {
                            builder.queryParam("category", category.trim().toLowerCase());
                        } else {
                            builder.queryParam("country", "us");
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !"ok".equalsIgnoreCase(response.path("status").asText())) {
                return List.of();
            }

            List<NewsItem> items = new ArrayList<>();
            for (JsonNode article : response.path("articles")) {
                String title = article.path("title").asText("");

                // Skip articles that have been retracted or purged by NewsAPI
                if (title.isBlank() || "[Removed]".equalsIgnoreCase(title)) {
                    continue;
                }

                String description = article.path("description").asText("");
                String url = article.path("url").asText("");
                Instant publishedAt = parsePublishedAt(article.path("publishedAt").asText(null));

                items.add(new NewsItem(title, description, url, publishedAt));
            }
            return items;

        } catch (RestClientException ex) {
            log.error("Failed to fetch headlines from NewsAPI: {}", ex.getMessage());
            return List.of();
        }
    }

    private Instant parsePublishedAt(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(dateStr);
        } catch (DateTimeParseException ex) {
            log.debug("Unparseable publication date '{}', using current time.", dateStr);
            return Instant.now();
        }
    }

    public record NewsItem(String title, String description, String url, Instant publishedAt) {
    }
}