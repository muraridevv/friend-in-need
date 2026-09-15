package com.friendinneed.conversation;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {
 List<ConversationMessage> findTop12ByProfileIdOrderByCreatedAtDesc(UUID profileId);
}
