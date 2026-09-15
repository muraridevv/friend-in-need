package com.friendinneed.conversation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

import com.friendinneed.emotion.EmotionLabel;

@Entity
public class ConversationMessage {
    @Id
    UUID id;
    UUID profileId;
    @Enumerated(EnumType.STRING)
    MessageRole role;
    @Column(columnDefinition = "text")
    String content;
    Instant createdAt;
    @Enumerated(EnumType.STRING)
    EmotionLabel emotion;
    Float emotionConfidence;

    protected ConversationMessage() {
    }

    public ConversationMessage(UUID profileId, MessageRole role, String content) {
        this.id = UUID.randomUUID();
        this.profileId = profileId;
        this.role = role;
        this.content = content;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public EmotionLabel getEmotion() {
        return emotion;
    }

    public Float getEmotionConfidence() {
        return emotionConfidence;
    }

    public void setEmotion(EmotionLabel emotion, double confidence) {
        this.emotion = emotion;
        this.emotionConfidence = (float) confidence;
    }

    public MessageRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }
}
