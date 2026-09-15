package com.friendinneed.conversation;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity
public class ConversationMessage {
 @Id UUID id; UUID profileId; @Enumerated(EnumType.STRING) MessageRole role; @Column(columnDefinition="text") String content; Instant createdAt;
 protected ConversationMessage() {}
 public ConversationMessage(UUID profileId, MessageRole role, String content) { this.id=UUID.randomUUID(); this.profileId=profileId; this.role=role; this.content=content; this.createdAt=Instant.now(); }
 public UUID getId(){return id;} public MessageRole getRole(){return role;} public String getContent(){return content;}
}
