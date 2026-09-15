package com.friendinneed.profile; import org.springframework.data.jpa.repository.JpaRepository; import java.util.UUID;
public interface CompanionProfileRepository extends JpaRepository<CompanionProfile, UUID> {}
