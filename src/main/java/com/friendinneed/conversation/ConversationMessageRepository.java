package com.friendinneed.conversation;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {
    List<ConversationMessage> findTop12ByProfileIdOrderByCreatedAtDesc(UUID profileId);
}
