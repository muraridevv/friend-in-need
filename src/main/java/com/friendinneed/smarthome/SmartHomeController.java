package com.friendinneed.smarthome;

import com.friendinneed.consent.*;
import com.friendinneed.profile.*;
import com.friendinneed.security.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;

import java.util.*;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/smarthome")
public class SmartHomeController {
    private final SmartHomeAdapter adapter;
    private final ConsentService consent;
    private final CompanionProfileRepository profiles;
    private final UserRepository users;

    public SmartHomeController(SmartHomeAdapter a, ConsentService c, CompanionProfileRepository p, UserRepository u) {
        adapter = a;
        consent = c;
        profiles = p;
        users = u;
    }

    private void allowed(UUID id) {
        String name = SecurityContextHolder.getContext().getAuthentication() == null ? null : SecurityContextHolder.getContext().getAuthentication().getName();
        if (name == null || !users.findByUsername(name).orElseThrow(() -> new AccessDeniedException("Authentication is required")).getId().equals(profiles.findById(id).orElseThrow(() -> new AccessDeniedException("Profile not found")).getUserId()))
            throw new AccessDeniedException("Access denied");
        if (!consent.isEnabled(id, IntegrationType.SMART_HOME))
            throw new org.springframework.security.access.AccessDeniedException("Smart home consent is required");
    }

    @GetMapping("/devices")
    List<DeviceState> devices(@RequestParam UUID profileId) {
        allowed(profileId);
        return adapter.listDevices();
    }

    @GetMapping("/devices/{entityId}")
    DeviceState device(@RequestParam UUID profileId, @PathVariable String entityId) {
        allowed(profileId);
        return adapter.getDevice(entityId);
    }

    @PostMapping("/devices/{entityId}/command")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void command(@RequestParam UUID profileId, @PathVariable String entityId, @RequestBody CommandRequest request) {
        allowed(profileId);
        adapter.sendCommand(entityId, request.command(), request.params() == null ? Map.of() : request.params());
    }

    public record CommandRequest(String command, Map<String, Object> params) {
    }
}
