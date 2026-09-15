package com.friendinneed.consent;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentRepository extends JpaRepository<IntegrationConsent, UUID> {
    Optional<IntegrationConsent> findByProfileIdAndIntegrationType(UUID profileId, IntegrationType integrationType);

    List<IntegrationConsent> findByProfileId(UUID profileId);
}
