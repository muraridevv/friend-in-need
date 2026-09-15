package com.friendinneed.consent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsentRepository extends JpaRepository<IntegrationConsent, UUID> {
    Optional<IntegrationConsent> findByProfileIdAndIntegrationType(UUID profileId, IntegrationType integrationType);

    List<IntegrationConsent> findByProfileId(UUID profileId);
}
