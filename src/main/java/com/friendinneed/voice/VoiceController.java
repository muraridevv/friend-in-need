package com.friendinneed.voice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/voice")
public class VoiceController {
    private static final Logger log = LoggerFactory.getLogger(VoiceController.class);
    private final VoiceService voice;

    public VoiceController(VoiceService voice) {
        this.voice = voice;
    }

    @PostMapping(value = "/transcriptions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Map<String, String> transcribe(@RequestPart("audio") MultipartFile audio) throws IOException {
        return Map.of("text", voice.transcribe(audio));
    }

    @PostMapping(value = "/speech", produces = "audio/mpeg")
    byte[] speech(@RequestBody SpeechRequest request) {
        return voice.speak(request.text());
    }

    @ExceptionHandler(RestClientResponseException.class)
    ResponseEntity<Map<String, String>> providerFailure(RestClientResponseException error) {
        log.warn("Voice provider request failed: status={}, response={}", error.getStatusCode(), error.getResponseBodyAsString());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "Voice provider rejected the request. Check VOICE_API_BASE_URL and voice model settings."));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, String>> unconfigured(IllegalStateException error) {
        log.warn("Voice requested without valid configuration: {}", error.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", error.getMessage()));
    }

    record SpeechRequest(String text) {
    }
}
