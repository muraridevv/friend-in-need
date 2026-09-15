package com.friendinneed.gesture;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
public class GestureEvent {
    @Id
    UUID id;
    UUID profileId;
    String gestureType;
    Instant createdAt;

    protected GestureEvent() {
    }

    public GestureEvent(UUID p, String t) {
        id = UUID.randomUUID();
        profileId = p;
        gestureType = t;
        createdAt = Instant.now();
    }
}
