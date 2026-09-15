package com.friendinneed.timer;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "companion_timer")
public class Timer {
    @Id
    private UUID id;
    private UUID profileId;
    private String label;
    private Instant triggerAt;
    private boolean fired;

    protected Timer() {
    }

    public Timer(UUID p, String l, Instant t) {
        id = UUID.randomUUID();
        profileId = p;
        label = l;
        triggerAt = t;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProfileId() {
        return profileId;
    }

    public String getLabel() {
        return label;
    }

    public Instant getTriggerAt() {
        return triggerAt;
    }

    public boolean isFired() {
        return fired;
    }

    public void markFired() {
        fired = true;
    }
}
