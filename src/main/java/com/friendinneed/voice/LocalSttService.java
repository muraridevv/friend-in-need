package com.friendinneed.voice;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class LocalSttService {
    private final RestClient client;

    public LocalSttService(@Value("${LOCAL_STT_URL:http://localhost:8178}") String baseUrl) {
        client = RestClient.builder().baseUrl(baseUrl).build();
    }

    public String transcribe(MultipartFile audio) throws IOException {
        try {
            var body = new LinkedMultiValueMap<String, Object>();
            body.add("file", new NamedBytes(audio.getBytes(), audio.getOriginalFilename()));
            JsonNode response = client.post().uri("/inference").contentType(MediaType.MULTIPART_FORM_DATA).body(body).retrieve().body(JsonNode.class);
            String text = response.path("text").asText(response.path("result").asText());
            if (text.isBlank()) throw new IllegalStateException("Local STT returned no transcription");
            return text;
        } catch (Exception error) {
            throw new IllegalStateException("Local STT server is unavailable", error);
        }
    }

    private static final class NamedBytes extends ByteArrayResource {
        private final String filename;

        NamedBytes(byte[] bytes, String filename) {
            super(bytes);
            this.filename = filename == null ? "recording.webm" : filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
