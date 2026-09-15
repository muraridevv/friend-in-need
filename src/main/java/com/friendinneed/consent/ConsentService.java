package com.friendinneed.consent;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ConsentService {
    private final ConsentRepository consents;

    public ConsentService(ConsentRepository consents) {
        this.consents = consents;
    }

    @Transactional
    public IntegrationConsent grant(UUID profileId, IntegrationType type) {
        IntegrationConsent consent = consents.findByProfileIdAndIntegrationType(profileId, type).orElseGet(() -> new IntegrationConsent(profileId, type));
        consent.grant();
        return consents.save(consent);
    }

    @Transactional
    public IntegrationConsent revoke(UUID profileId, IntegrationType type) {
        IntegrationConsent consent = consents.findByProfileIdAndIntegrationType(profileId, type).orElseGet(() -> new IntegrationConsent(profileId, type));
        consent.revoke();
        return consents.save(consent);
    }

    public boolean isEnabled(UUID profileId, IntegrationType type) {
        return consents.findByProfileIdAndIntegrationType(profileId, type).map(IntegrationConsent::isEnabled).orElse(false);
    }

    public List<IntegrationConsent> all(UUID profileId) {
        return consents.findByProfileId(profileId);
    }
}
