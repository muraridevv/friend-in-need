package com.friendinneed.voice;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final VoiceStreamEndpoint endpoint;
    private final com.friendinneed.robot.RobotWebSocketEndpoint robot;
    private final String[] origins;

    public WebSocketConfig(VoiceStreamEndpoint endpoint, com.friendinneed.robot.RobotWebSocketEndpoint robot, @Value("${cors.allowed-origins:http://localhost:8080}") String allowedOrigins) {
        this.endpoint = endpoint;
        this.robot = robot;
        this.origins = allowedOrigins.split(",");
    }

    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(endpoint, "/ws/voice").setAllowedOrigins(origins);
        registry.addHandler(robot, "/ws/hardware").setAllowedOrigins(origins);
    }
}
