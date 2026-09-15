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
    @Column(columnDefinition = "text") private String embedding;
    private String embeddingModel;
    private Instant createdAt;
    protected CompanionMemory() { }
    public CompanionMemory(UUID profileId, String content, int importance, String embedding, String embeddingModel) { this.id=UUID.randomUUID(); this.profileId=profileId; this.content=content; this.importance=importance; this.embedding=embedding; this.embeddingModel=embeddingModel; this.createdAt=Instant.now(); }
    public String getContent() { return content; }
    public String getEmbedding() { return embedding; }
}
