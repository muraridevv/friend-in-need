package com.friendinneed.voice;
import org.springframework.context.annotation.Configuration; import org.springframework.web.socket.config.annotation.*;
@Configuration @EnableWebSocket public class WebSocketConfig implements WebSocketConfigurer { private final VoiceStreamEndpoint endpoint; public WebSocketConfig(VoiceStreamEndpoint endpoint){this.endpoint=endpoint;} public void registerWebSocketHandlers(WebSocketHandlerRegistry registry){registry.addHandler(endpoint,"/ws/voice").setAllowedOriginPatterns("*");} }
