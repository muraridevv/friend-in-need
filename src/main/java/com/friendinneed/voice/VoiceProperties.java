package com.friendinneed.voice;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties("voice")
public record VoiceProperties(String baseUrl, String apiKey, String sttModel, String ttsModel, String ttsVoice) { }
