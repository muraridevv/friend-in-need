package com.friendinneed.smarthome;

import com.fasterxml.jackson.databind.*;

import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@ConditionalOnProperty(name = "smarthome.type", havingValue = "homeassistant")
public class HomeAssistantAdapter implements SmartHomeAdapter {
    private final RestClient client;

    public HomeAssistantAdapter(@Value("${smarthome.base-url}") String url, @Value("${smarthome.api-key}") String key) {
        client = RestClient.builder().baseUrl(url).defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + key).build();
    }

    public List<DeviceState> listDevices() {
        JsonNode n = client.get().uri("/api/states").retrieve().body(JsonNode.class);
        List<DeviceState> r = new ArrayList<>();
        n.forEach(x -> r.add(state(x)));
        return r;
    }

    public DeviceState getDevice(String id) {
        return state(client.get().uri("/api/states/{id}", id).retrieve().body(JsonNode.class));
    }

    public void sendCommand(String id, String command, Map<String, Object> params) {
        String domain = id.substring(0, id.indexOf('.'));
        Map<String, Object> body = new HashMap<>(params);
        body.put("entity_id", id);
        client.post().uri("/api/services/{domain}/{command}", domain, command).body(body).retrieve().toBodilessEntity();
    }

    public Map<String, Object> getSensorData(String id) {
        return getDevice(id).attributes();
    }

    private DeviceState state(JsonNode n) {
        Map<String, Object> a = new ObjectMapper().convertValue(n.path("attributes"), Map.class);
        return new DeviceState(n.path("entity_id").asText(), n.path("attributes").path("friendly_name").asText(n.path("entity_id").asText()), n.path("state").asText(), a);
    }
}
