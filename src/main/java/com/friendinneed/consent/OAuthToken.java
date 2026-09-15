package com.friendinneed.consent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.UUID;

@Entity
public class OAuthToken {
    @Id
    private UUID id;
    private UUID profileId;
    private String provider;
    @Column(columnDefinition = "text")
    private String encryptedAccessToken;
    @Column(columnDefinition = "text")
    private String encryptedRefreshToken;
    private Instant expiresAt;

    protected OAuthToken() {
    }
}
