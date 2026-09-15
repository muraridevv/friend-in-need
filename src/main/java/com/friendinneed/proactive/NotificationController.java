package com.friendinneed.proactive;

import com.friendinneed.profile.CompanionProfile;
import com.friendinneed.profile.CompanionProfileRepository;
import com.friendinneed.security.UserRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/profiles")
public class NotificationController {
    private final CompanionProfileRepository profiles; private final UserRepository users;
    private final ProactiveMessageRepository messages; private final NotificationPublisher notifications; private final Clock clock;
    public NotificationController(CompanionProfileRepository profiles, UserRepository users, ProactiveMessageRepository messages, NotificationPublisher notifications, Clock clock) {
        this.profiles = profiles; this.users = users; this.messages = messages; this.notifications = notifications; this.clock = clock;
    }
    @GetMapping(value = "/{id}/notifications", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter notifications(@PathVariable UUID id) {
        verifyOwner(id);
        SseEmitter emitter = notifications.connect(id);
        messages.findByProfileIdAndReadAtIsNullOrderByCreatedAtDesc(id).stream().filter(message -> message.getDeliveredAt() == null).forEach(message -> {
            try { emitter.send(SseEmitter.event().name("proactive").data(message.getContent())); message.markDelivered(clock.instant()); }
            catch (Exception exception) { emitter.completeWithError(exception); }
        });
        return emitter;
    }
    private void verifyOwner(UUID id) {
        String username = SecurityContextHolder.getContext().getAuthentication() == null ? null : SecurityContextHolder.getContext().getAuthentication().getName();
        if (username == null) throw new AccessDeniedException("Authentication is required");
        UUID userId = users.findByUsername(username).orElseThrow(() -> new AccessDeniedException("Authenticated user was not found")).getId();
        CompanionProfile profile = profiles.findById(id).orElseThrow(() -> new AccessDeniedException("Profile not found"));
        if (!userId.equals(profile.getUserId())) throw new AccessDeniedException("Profile does not belong to the authenticated user");
    }
}
