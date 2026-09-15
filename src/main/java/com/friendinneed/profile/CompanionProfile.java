package com.friendinneed.profile;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity
public class CompanionProfile {
 @Id UUID id; String displayName; @Column(columnDefinition="text") String personality; @Column(columnDefinition="text") String interests; String timezone; String faceFingerprint; Instant faceEnrolledAt; Instant createdAt;
 protected CompanionProfile() {} public CompanionProfile(String name, String personality, String interests, String timezone) { id=UUID.randomUUID(); displayName=name; this.personality=personality; this.interests=interests; this.timezone=timezone; createdAt=Instant.now(); }
 public UUID getId(){return id;} public String getDisplayName(){return displayName;} public String getPersonality(){return personality;} public String getInterests(){return interests;} public String getTimezone(){return timezone;}
 public void enrollFace(String fingerprint){faceFingerprint=fingerprint;faceEnrolledAt=Instant.now();} public boolean faceMatches(String fingerprint){return faceFingerprint != null && faceFingerprint.equals(fingerprint);}
 public boolean hasFaceEnrollment(){return faceFingerprint != null;}
}
