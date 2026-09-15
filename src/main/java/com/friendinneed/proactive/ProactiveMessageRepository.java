package com.friendinneed.proactive;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProactiveMessageRepository extends JpaRepository<ProactiveMessage, UUID> {
    List<ProactiveMessage> findByProfileIdAndReadAtIsNullOrderByCreatedAtDesc(UUID profileId);
}
