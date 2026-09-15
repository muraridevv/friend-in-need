package com.friendinneed.routing;

import com.fasterxml.jackson.databind.JsonNode;
import com.friendinneed.conversation.CompanionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OllamaAdapter {

    private final RestClient client;
    private final String model;
    private final String baseUrl;

    @Autowired
    public OllamaAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${OLLAMA_BASE_URL:http://localhost:11434}") String baseUrl,
            @Value("${OLLAMA_MODEL:phi3:mini}") String model) {
        this.baseUrl = baseUrl;
        this.model = model;
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl + "v1" : baseUrl + "/v1";
        this.client = restClientBuilder.baseUrl(normalizedBaseUrl).build();
    }

    // Secondary constructor for unit testing
    OllamaAdapter(RestClient client, String model) {
        this.client = client;
        this.model = model;
        this.baseUrl = null;
    }

    /**
     * Compatibility entry point matching CompanionService.talk; context is used as the local system prompt.
     */
    public CompanionService.Reply talk(UUID profileId, String text, String context) {
        return new CompanionService.Reply(talk(context, text));
    }

    public String talk(String systemPrompt, String text) {
        JsonNode response = client.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request(systemPrompt, text, false))
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            return "";
        }
        return response.path("choices").path(0).path("message").path("content").asText();
    }

    public Flux<String> talkStreaming(String systemPrompt, String text) {
        return Flux.defer(() -> Flux.just(talk(systemPrompt, text)));
    }

    private Map<String, Object> request(String systemPrompt, String text, boolean stream) {
        return Map.of(
                "model", model,
                "stream", stream,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt != null ? systemPrompt : ""),
                        Map.of("role", "user", "content", text != null ? text : "")
                )
        );
    }
}