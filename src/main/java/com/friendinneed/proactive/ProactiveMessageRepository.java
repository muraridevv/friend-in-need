package com.friendinneed.proactive;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProactiveMessageRepository extends JpaRepository<ProactiveMessage, UUID> {
    List<ProactiveMessage> findByProfileIdAndReadAtIsNullOrderByCreatedAtDesc(UUID profileId);
}
