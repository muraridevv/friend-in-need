package com.friendinneed.routing;

import com.fasterxml.jackson.databind.JsonNode;
import com.friendinneed.conversation.CompanionService;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Flux;

@Service
public class OllamaAdapter {
    private final RestClient client;
    private final String model;
    private final String baseUrl;
    public OllamaAdapter(@Value("${OLLAMA_BASE_URL:http://localhost:11434}") String baseUrl,
            @Value("${OLLAMA_MODEL:phi3:mini}") String model) {
        this(RestClient.builder().baseUrl(baseUrl + (baseUrl.endsWith("/") ? "v1" : "/v1")).build(), model);
    }
    OllamaAdapter(RestClient client, String model) { this.client = client; this.model = model; this.baseUrl = null; }
    /** Compatibility entry point matching CompanionService.talk; context is used as the local system prompt. */
    public CompanionService.Reply talk(UUID profileId, String text, String context) {
        return new CompanionService.Reply(talk(context, text));
    }
    public String talk(String systemPrompt, String text) {
        JsonNode response = client.post().uri("/chat/completions").contentType(MediaType.APPLICATION_JSON)
                .body(request(systemPrompt, text, false)).retrieve().body(JsonNode.class);
        return response.path("choices").path(0).path("message").path("content").asText();
    }
    public Flux<String> talkStreaming(String systemPrompt, String text) {
        // Ollama's OpenAI-compatible endpoint streams SSE responses; the non-streaming response keeps this fallback reliable.
        return Flux.defer(() -> Flux.just(talk(systemPrompt, text)));
    }
    private Map<String, Object> request(String systemPrompt, String text, boolean stream) {
        return Map.of("model", model, "stream", stream, "messages", List.of(
                Map.of("role", "system", "content", systemPrompt), Map.of("role", "user", "content", text)));
    }
}
