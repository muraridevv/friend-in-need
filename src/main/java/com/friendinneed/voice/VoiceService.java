package com.friendinneed.voice;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;

/** OpenAI-compatible voice adapter. Its base URL is deliberately separate from OpenRouter. */
@Service
public class VoiceService {
    private final VoiceProperties properties; private final RestClient client;
    public VoiceService(VoiceProperties properties) { this.properties=properties; this.client=RestClient.builder().baseUrl(properties.baseUrl() == null || properties.baseUrl().isBlank() ? "http://localhost" : properties.baseUrl()).build(); }
    public String transcribe(MultipartFile audio) throws IOException {
        configured(); var body=new LinkedMultiValueMap<String,Object>();
        body.add("model",properties.sttModel()); body.add("file",new NamedBytes(audio.getBytes(), audio.getOriginalFilename()));
        JsonNode response=client.post().uri("/audio/transcriptions").header(HttpHeaders.AUTHORIZATION,"Bearer "+properties.apiKey()).contentType(MediaType.MULTIPART_FORM_DATA).body(body).retrieve().body(JsonNode.class);
        return response.path("text").asText();
    }
    public byte[] speak(String text) {
        configured(); return client.post().uri("/audio/speech").header(HttpHeaders.AUTHORIZATION,"Bearer "+properties.apiKey()).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("model",properties.ttsModel(),"voice",properties.ttsVoice(),"input",text,"response_format","mp3")).retrieve().body(byte[].class);
    }
    private void configured() { if (properties.baseUrl()==null || properties.baseUrl().isBlank() || properties.apiKey()==null || properties.apiKey().isBlank()) throw new IllegalStateException("Voice provider is not configured"); }
    private static final class NamedBytes extends ByteArrayResource { private final String filename; NamedBytes(byte[] bytes,String filename) { super(bytes); this.filename=filename == null ? "recording.webm" : filename; } @Override public String getFilename() { return filename; } }
}
