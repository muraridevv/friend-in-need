package com.friendinneed.presence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PresenceEventRepository extends JpaRepository<PresenceEvent, UUID> {
    Optional<PresenceEvent> findTop1ByProfileIdOrderByCreatedAtDesc(UUID profileId);
}
