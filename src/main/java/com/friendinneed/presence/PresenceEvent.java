package com.friendinneed.presence;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
public class PresenceEvent {
    @Id
    private UUID id;
    private UUID profileId;
    @Enumerated(EnumType.STRING)
    private PresenceStatus status;
    private Integer faceCount;
    private Instant createdAt;

    protected PresenceEvent() {
    }

    public PresenceEvent(UUID profileId, PresenceStatus status, Integer faceCount, Instant createdAt) {
        id = UUID.randomUUID();
        this.profileId = profileId;
        this.status = status;
        this.faceCount = faceCount;
        this.createdAt = createdAt;
    }

    public PresenceStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
