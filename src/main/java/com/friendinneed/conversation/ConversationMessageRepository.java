package com.friendinneed.conversation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {
    List<ConversationMessage> findTop12ByProfileIdOrderByCreatedAtDesc(UUID profileId);

    int deleteByCreatedAtBefore(Instant cutoff);

    List<ConversationMessage> findByProfileIdOrderByCreatedAtAsc(UUID profileId);

    long deleteByProfileId(UUID profileId);

    List<ConversationMessage> findTop20ByProfileIdOrderByCreatedAtDesc(UUID profileId);
}
