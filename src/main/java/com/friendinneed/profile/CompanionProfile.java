package com.friendinneed.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
public class CompanionProfile {
    @Id
    private UUID id;
    @Column(name = "user_id")
    private UUID userId;
    private String displayName;
    @Column(columnDefinition = "text")
    private String personality;
    @Column(columnDefinition = "text")
    private String interests;
    private String timezone;
    private String location;
    private String faceFingerprint;
    private Instant faceEnrolledAt;
    private Instant createdAt;
    private boolean proactiveEnabled;

    protected CompanionProfile() {
    }

    public CompanionProfile(UUID userId, String displayName, String personality, String interests, String timezone, String location) {
        this.userId = userId;
        this.id = UUID.randomUUID();
        this.displayName = displayName;
        this.personality = personality;
        this.interests = interests == null ? "" : interests;
        this.timezone = timezone;
        this.location = location;
        this.createdAt = Instant.now();
    }

    public CompanionProfile(String displayName, String personality, String interests, String timezone, String location) {
        this(null, displayName, personality, interests, timezone, location);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public boolean isProactiveEnabled() {
        return proactiveEnabled;
    }

    public void setProactiveEnabled(boolean proactiveEnabled) {
        this.proactiveEnabled = proactiveEnabled;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPersonality() {
        return personality;
    }

    public String getInterests() {
        return interests;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getLocation() {
        return location;
    }

    public void update(String displayName, String personality, String interests, String timezone, String location) {
        this.displayName = displayName;
        this.personality = personality;
        this.interests = interests == null ? "" : interests;
        this.timezone = timezone;
        this.location = location;
    }

    public void enrollFace(String fingerprint) {
        faceFingerprint = fingerprint;
        faceEnrolledAt = Instant.now();
    }

    public FaceMatch faceMatch(String submittedTemplate) {
        if (faceFingerprint == null || submittedTemplate == null) return new FaceMatch(false, 0);
        double best = 0;
        for (String enrolled : faceFingerprint.split("\\|"))
            for (String submitted : submittedTemplate.split("\\|")) {
                if (enrolled.length() != submitted.length()) continue;
                int equalBits = 0;
                for (int index = 0; index < enrolled.length(); index++)
                    if (enrolled.charAt(index) == submitted.charAt(index)) equalBits++;
                best = Math.max(best, (double) equalBits / enrolled.length());
            }
        // Template matching is only a convenience signal; it is not authentication.
        return new FaceMatch(best >= 0.68, Math.round(best * 100));
    }

    public boolean hasFaceEnrollment() {
        return faceFingerprint != null;
    }

    public record FaceMatch(boolean recognized, long confidence) {
    }
}
