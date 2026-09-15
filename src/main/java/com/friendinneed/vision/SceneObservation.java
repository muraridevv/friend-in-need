package com.friendinneed.vision;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
public class SceneObservation {
    @Id
    UUID id;
    UUID profileId;
    @Column(columnDefinition = "text")
    String description;
    Instant createdAt;

    protected SceneObservation() {
    }

    public SceneObservation(UUID p, String d) {
        id = UUID.randomUUID();
        profileId = p;
        description = d;
        createdAt = Instant.now();
    }
}
