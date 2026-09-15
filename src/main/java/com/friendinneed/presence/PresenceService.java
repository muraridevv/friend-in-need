package com.friendinneed.presence;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class PresenceService {
    private final PresenceEventRepository events;
    private final Clock clock;

    public PresenceService(PresenceEventRepository events, Clock clock) {
        this.events = events;
        this.clock = clock;
    }

    public PresenceChange recordPresence(UUID profileId, PresenceStatus status, Integer faces) {
        PresenceEvent previous = events.findTop1ByProfileIdOrderByCreatedAtDesc(profileId).orElse(null);
        if (previous != null && previous.getStatus() == status)
            return new PresenceChange(false, status, status, null, faces != null && faces > 1);
        Instant now = clock.instant();
        events.save(new PresenceEvent(profileId, status, faces, now));
        Duration away = previous != null && previous.getStatus() == PresenceStatus.AWAY && status == PresenceStatus.PRESENT ? Duration.between(previous.getCreatedAt(), now) : null;
        return new PresenceChange(true, previous == null ? null : previous.getStatus(), status, away, faces != null && faces > 1);
    }

    public record PresenceChange(boolean changed, PresenceStatus previousStatus, PresenceStatus newStatus,
                                 Duration awayDuration, boolean multiplePeople) {
    }
}
