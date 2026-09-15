package com.friendinneed.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class OllamaAdapterTest {
    @Test void sendsOpenAiCompatibleChatCompletionRequest() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"choices\":[{\"message\":{\"content\":\"hello\"}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json"); exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response); exchange.close();
        });
        server.start();
        try {
            OllamaAdapter adapter = new OllamaAdapter(RestClient.builder().baseUrl("http://localhost:" + server.getAddress().getPort() + "/v1").build(), "phi3:mini");
            assertEquals("hello", adapter.talk("be kind", "hi"));
            org.junit.jupiter.api.Assertions.assertTrue(requestBody.get().contains("\"model\":\"phi3:mini\""));
            org.junit.jupiter.api.Assertions.assertTrue(requestBody.get().contains("\"role\":\"system\""));
            org.junit.jupiter.api.Assertions.assertTrue(requestBody.get().contains("\"role\":\"user\""));
        } finally { server.stop(0); }
    }
}
