package com.friendinneed.profile;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanionProfileRepository extends JpaRepository<CompanionProfile, UUID> {
    List<CompanionProfile> findByFaceFingerprintIsNotNull();
    List<CompanionProfile> findByUserIdAndFaceFingerprintIsNotNull(UUID userId);
    List<CompanionProfile> findByProactiveEnabledTrue();
}
