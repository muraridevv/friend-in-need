package com.friendinneed.profile; import org.springframework.data.jpa.repository.JpaRepository; import java.util.UUID; import java.util.List;
public interface CompanionProfileRepository extends JpaRepository<CompanionProfile, UUID> { List<CompanionProfile> findByFaceFingerprintIsNotNull(); }
