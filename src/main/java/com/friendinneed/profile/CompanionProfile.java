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
    public void enrollFace(String fingerprint) { faceFingerprint = fingerprint; faceEnrolledAt = Instant.now(); }
    public boolean faceMatches(String descriptor) {
        if (faceFingerprint == null || descriptor == null || faceFingerprint.length() != descriptor.length()) return false;
        int sameBits = 0;
        for (int index = 0; index < descriptor.length(); index++) if (faceFingerprint.charAt(index) == descriptor.charAt(index)) sameBits++;
        return (double) sameBits / descriptor.length() >= 0.88;
    }
    public boolean hasFaceEnrollment() { return faceFingerprint != null; }
}
