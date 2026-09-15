package com.friendinneed.voice;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import com.friendinneed.emotion.EmotionLabel;

/** OpenAI-compatible voice adapter. Its base URL is deliberately separate from OpenRouter. */
@Service
public class VoiceService {
    private static final Logger log = LoggerFactory.getLogger(VoiceService.class);
    private final VoiceProperties properties; private final RestClient client;
    public VoiceService(VoiceProperties properties) { this.properties=properties; String baseUrl = normalizeBaseUrl(properties.baseUrl()); this.client=RestClient.builder().baseUrl(baseUrl).build(); log.info("Voice provider configured: baseUrl={}, sttEndpoint={}audio/transcriptions, ttsEndpoint={}audio/speech", baseUrl, baseUrl, baseUrl); }
    public String transcribe(MultipartFile audio) throws IOException {
        configured(); log.info("Requesting STT transcription: model={}, contentType={}, sizeBytes={}", properties.sttModel(), audio.getContentType(), audio.getSize()); var body=new LinkedMultiValueMap<String,Object>();
        body.add("model",properties.sttModel()); body.add("file",new NamedBytes(audio.getBytes(), audio.getOriginalFilename()));
        JsonNode response=client.post().uri("audio/transcriptions").header(HttpHeaders.AUTHORIZATION,"Bearer "+properties.apiKey()).contentType(MediaType.MULTIPART_FORM_DATA).body(body).retrieve().body(JsonNode.class);
        String text = response.path("text").asText(); log.info("STT transcription completed: textLength={}", text.length()); return text;
    }
    public byte[] speak(String text) { return speak(text, null); }
    public byte[] speak(String text, EmotionLabel emotion) {
        configured(); log.info("Requesting TTS synthesis: model={}, voice={}, textLength={}", properties.ttsModel(), properties.ttsVoice(), text.length()); return client.post().uri("audio/speech").header(HttpHeaders.AUTHORIZATION,"Bearer "+properties.apiKey()).contentType(MediaType.APPLICATION_JSON)
                .body(ttsBody(text, emotion)).retrieve().body(byte[].class);
    }
    private Map<String, Object> ttsBody(String text, EmotionLabel emotion) {
        String voice = emotion == EmotionLabel.JOY ? "shimmer" : properties.ttsVoice();
        double speed = emotion == EmotionLabel.JOY ? 1.1 : emotion == EmotionLabel.SADNESS ? 0.9 : 1.0;
        String instructions = emotion == EmotionLabel.ANGER ? "Speak in a calm, soothing tone" : emotion == EmotionLabel.SADNESS ? "Speak gently and warmly" : "";
        return Map.of("model", properties.ttsModel(), "voice", voice, "input", text, "response_format", "mp3", "speed", speed, "instructions", instructions);
    }
    private String normalizeBaseUrl(String configuredBaseUrl) {
        if (configuredBaseUrl == null || configuredBaseUrl.isBlank()) return "http://localhost/";
        URI uri = URI.create(configuredBaseUrl);
        String path = uri.getPath() == null ? "" : uri.getPath();
        // OpenAI's public API requires /v1. Preserve explicit provider paths for compatible APIs.
        if ("api.openai.com".equalsIgnoreCase(uri.getHost()) && (path.isBlank() || "/".equals(path))) path = "/v1";
        String normalized = uri.getScheme() + "://" + uri.getAuthority() + path;
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }
    private void configured() { if (properties.baseUrl()==null || properties.baseUrl().isBlank() || properties.apiKey()==null || properties.apiKey().isBlank()) throw new IllegalStateException("Voice provider is not configured"); }
    private static final class NamedBytes extends ByteArrayResource { private final String filename; NamedBytes(byte[] bytes,String filename) { super(bytes); this.filename=filename == null ? "recording.webm" : filename; } @Override public String getFilename() { return filename; } }
}
