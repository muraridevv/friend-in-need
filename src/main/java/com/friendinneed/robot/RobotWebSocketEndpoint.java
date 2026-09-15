package com.friendinneed.robot;

import com.fasterxml.jackson.databind.*;

import java.util.*;
import java.util.concurrent.*;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class RobotWebSocketEndpoint extends TextWebSocketHandler {
    private final ObjectMapper json;
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private volatile double distance = 100;
    private volatile Map<String, Boolean> touch = Map.of();
    private volatile byte[] image;

    public RobotWebSocketEndpoint(ObjectMapper j) {
        json = j;
    }

    public void afterConnectionEstablished(WebSocketSession s) {
        sessions.add(s);
    }

    public void afterConnectionClosed(WebSocketSession s, CloseStatus c) {
        sessions.remove(s);
    }

    protected void handleTextMessage(WebSocketSession s, TextMessage m) throws Exception {
        JsonNode n = json.readTree(m.getPayload());
        if ("sensorUpdate".equals(n.path("type").asText())) {
            distance = n.path("distance").asDouble(100);
            touch = json.convertValue(n.path("touch"), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Boolean>>() {
            });
        }
        if ("imageCapture".equals(n.path("type").asText()))
            image = Base64.getDecoder().decode(n.path("imageBase64").asText());
    }

    public void send(Map<String, Object> value) {
        sessions.removeIf(s -> {
            try {
                s.sendMessage(new TextMessage(json.writeValueAsString(value)));
                return false;
            } catch (Exception e) {
                return true;
            }
        });
    }

    public double distance() {
        return distance;
    }

    public boolean touch(String zone) {
        return touch.getOrDefault(zone, false);
    }

    public byte[] requestImage() {
        send(Map.of("type", "captureImage"));
        return image;
    }
}
