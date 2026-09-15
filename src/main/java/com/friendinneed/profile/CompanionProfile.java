package com.friendinneed.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;

@Entity
public class CompanionProfile {
    @Id private UUID id;
    private String displayName;
    @Column(columnDefinition = "text") private String personality;
    @Column(columnDefinition = "text") private String interests;
    private String timezone;
    private String location;
    private String faceFingerprint;
    private Instant faceEnrolledAt;
    private Instant createdAt;

    protected CompanionProfile() { }

    public CompanionProfile(String displayName, String personality, String interests, String timezone, String location) {
        this.id = UUID.randomUUID();
        this.displayName = displayName;
        this.personality = personality;
        this.interests = interests == null ? "" : interests;
        this.timezone = timezone;
        this.location = location;
        this.createdAt = Instant.now();
    }
    public UUID getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getPersonality() { return personality; }
    public String getInterests() { return interests; }
    public String getTimezone() { return timezone; }
    public String getLocation() { return location; }
    public void update(String displayName, String personality, String interests, String timezone, String location) {
        this.displayName = displayName;
        this.personality = personality;
        this.interests = interests == null ? "" : interests;
        this.timezone = timezone;
        this.location = location;
    }
    public void enrollFace(String fingerprint) { faceFingerprint = fingerprint; faceEnrolledAt = Instant.now(); }
    public FaceMatch faceMatch(String submittedTemplate) {
        if (faceFingerprint == null || submittedTemplate == null) return new FaceMatch(false, 0);
        double best = 0;
        for (String enrolled : faceFingerprint.split("\\|")) for (String submitted : submittedTemplate.split("\\|")) {
            if (enrolled.length() != submitted.length()) continue;
            int equalBits = 0;
            for (int index = 0; index < enrolled.length(); index++) if (enrolled.charAt(index) == submitted.charAt(index)) equalBits++;
            best = Math.max(best, (double) equalBits / enrolled.length());
        }
        // Template matching is only a convenience signal; it is not authentication.
        return new FaceMatch(best >= 0.68, Math.round(best * 100));
    }
    public record FaceMatch(boolean recognized, long confidence) { }
    public boolean hasFaceEnrollment() { return faceFingerprint != null; }
}
