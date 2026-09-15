package com.friendinneed.timer;

import com.friendinneed.profile.CompanionProfileRepository;
import com.friendinneed.security.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/profiles/{profileId}/timers")
public class TimerController {
    private final TimerService service;
    private final CompanionProfileRepository profiles;
    private final UserRepository users;

    public TimerController(TimerService s, CompanionProfileRepository p, UserRepository u) {
        service = s;
        profiles = p;
        users = u;
    }

    private void owner(UUID p) {
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!users.findByUsername(name).orElseThrow(() -> new AccessDeniedException("Authentication is required")).getId().equals(profiles.findById(p).orElseThrow(() -> new AccessDeniedException("Profile not found")).getUserId()))
            throw new AccessDeniedException("Access denied");
    }

    @GetMapping
    List<Timer> list(@PathVariable UUID profileId) {
        owner(profileId);
        return service.list(profileId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Timer create(@PathVariable UUID profileId, @RequestBody Request r) {
        owner(profileId);
        return service.create(profileId, r.label(), r.triggerAt());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID profileId, @PathVariable UUID id) {
        owner(profileId);
        service.delete(profileId, id);
    }

    record Request(String label, Instant triggerAt) {
    }
}
