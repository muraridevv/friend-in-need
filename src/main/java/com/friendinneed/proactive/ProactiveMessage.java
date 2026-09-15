package com.friendinneed.proactive;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;

@Entity
public class ProactiveMessage {
    @Id private UUID id;
    private UUID profileId;
    @Column(columnDefinition = "text") private String content;
    private Instant createdAt;
    private Instant deliveredAt;
    private Instant readAt;
    protected ProactiveMessage() { }
    public ProactiveMessage(UUID profileId, String content, Instant now) { this.id = UUID.randomUUID(); this.profileId = profileId; this.content = content; this.createdAt = now; }
    public UUID getId() { return id; }
    public UUID getProfileId() { return profileId; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public void markDelivered(Instant now) { deliveredAt = now; }
}
