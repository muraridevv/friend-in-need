package com.friendinneed.routine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "routine")
public class Routine {
    @Id
    private UUID id;
    private UUID profileId;
    private String triggerPhrase;
    @Column(columnDefinition = "text")
    private String steps;

    protected Routine() {
    }

    public Routine(UUID p, String t, String s) {
        id = UUID.randomUUID();
        profileId = p;
        triggerPhrase = t;
        steps = s;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProfileId() {
        return profileId;
    }

    public String getTriggerPhrase() {
        return triggerPhrase;
    }

    public String getSteps() {
        return steps;
    }
}
