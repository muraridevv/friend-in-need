package com.friendinneed.voice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class LocalTtsService {
    private final RestClient client;

    public LocalTtsService(@Value("${LOCAL_TTS_URL:http://localhost:5000}") String baseUrl) {
        client = RestClient.builder().baseUrl(baseUrl).build();
    }

    public byte[] speak(String text) {
        try {
            return client.post().uri("/synthesize").contentType(MediaType.APPLICATION_JSON).body(Map.of("text", text)).retrieve().body(byte[].class);
        } catch (Exception error) {
            throw new IllegalStateException("Local TTS server is unavailable", error);
        }
    }
}
