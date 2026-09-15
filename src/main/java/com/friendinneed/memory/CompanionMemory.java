package com.friendinneed.memory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
public class CompanionMemory {
    @Id
    private UUID id;
    private UUID profileId;
    @Column(columnDefinition = "text")
    private String content;
    private int importance;
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Column(columnDefinition = "vector(1536)")
    private float[] embedding;
    private Instant createdAt;

    protected CompanionMemory() {
    }

    public CompanionMemory(UUID profileId, String content, int importance, float[] embedding) {
        this.id = UUID.randomUUID();
        this.profileId = profileId;
        this.content = content;
        this.importance = importance;
        this.embedding = embedding;
        this.createdAt = Instant.now();
    }

    public String getContent() {
        return content;
    }

    public float[] getEmbedding() {
        return embedding;
    }
}
