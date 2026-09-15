package com.friendinneed.memory;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
public class CompanionMemory {
    @Id private UUID id;
    private UUID profileId;
    @Column(columnDefinition = "text") private String content;
    private int importance;
    private Instant createdAt;
    protected CompanionMemory() { }
    public CompanionMemory(UUID profileId, String content, int importance) { this.id=UUID.randomUUID(); this.profileId=profileId; this.content=content; this.importance=importance; this.createdAt=Instant.now(); }
    public String getContent() { return content; }
}
