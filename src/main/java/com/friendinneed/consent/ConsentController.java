package com.friendinneed.consent;

import com.friendinneed.profile.CompanionProfileRepository;
import com.friendinneed.security.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/profiles")
public class ConsentController {
    private final ConsentService service;
    private final CompanionProfileRepository profiles;
    private final UserRepository users;

    public ConsentController(ConsentService service, CompanionProfileRepository profiles, UserRepository users) {
        this.service = service;
        this.profiles = profiles;
        this.users = users;
    }

    @GetMapping("/{id}/consents")
    List<IntegrationConsent> all(@PathVariable UUID id) {
        owner(id);
        return service.all(id);
    }

    @PutMapping("/{id}/consents/{type}")
    IntegrationConsent update(@PathVariable UUID id, @PathVariable IntegrationType type, @Valid @RequestBody ConsentRequest request) {
        owner(id);
        return request.enabled() ? service.grant(id, type) : service.revoke(id, type);
    }

    private void owner(UUID id) {
        String name = SecurityContextHolder.getContext().getAuthentication() == null ? null : SecurityContextHolder.getContext().getAuthentication().getName();
        if (name == null || name.isBlank()) throw new AccessDeniedException("Authentication is required");
        UUID user = users.findByUsername(name).orElseThrow(() -> new AccessDeniedException("Authentication is required")).getId();
        if (!user.equals(profiles.findById(id).orElseThrow(() -> new AccessDeniedException("Profile not found")).getUserId()))
            throw new AccessDeniedException("Profile does not belong to the authenticated user");
    }

    record ConsentRequest(boolean enabled) {
    }
}
