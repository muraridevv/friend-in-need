package com.friendinneed.consent;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
public class IntegrationConsent {
    @Id
    private UUID id;
    private UUID profileId;
    @Enumerated(EnumType.STRING)
    private IntegrationType integrationType;
    private boolean enabled;
    private Instant grantedAt;
    private Instant revokedAt;

    protected IntegrationConsent() {
    }

    public IntegrationConsent(UUID profileId, IntegrationType type) {
        id = UUID.randomUUID();
        this.profileId = profileId;
        integrationType = type;
    }

    public void grant() {
        enabled = true;
        grantedAt = Instant.now();
        revokedAt = null;
    }

    public void revoke() {
        enabled = false;
        revokedAt = Instant.now();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public IntegrationType getIntegrationType() {
        return integrationType;
    }
}
