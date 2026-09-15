package com.friendinneed.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CompanionProfileRepository extends JpaRepository<CompanionProfile, UUID> {
    List<CompanionProfile> findByFaceFingerprintIsNotNull();

    List<CompanionProfile> findByUserIdAndFaceFingerprintIsNotNull(UUID userId);

    List<CompanionProfile> findByProactiveEnabledTrue();
}
