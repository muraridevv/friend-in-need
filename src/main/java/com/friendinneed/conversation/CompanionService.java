package com.friendinneed.conversation;

import com.friendinneed.memory.MemoryService; import com.friendinneed.profile.CompanionProfile; import com.friendinneed.profile.CompanionProfileRepository;
import org.springframework.ai.chat.client.ChatClient; import org.slf4j.Logger; import org.slf4j.LoggerFactory; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.util.*; import java.util.stream.Collectors;

@Service
public class CompanionService {
 private static final Logger log=LoggerFactory.getLogger(CompanionService.class);
 private final ChatClient chat; private final ConversationMessageRepository messages; private final CompanionProfileRepository profiles; private final MemoryService memory;
 public CompanionService(ChatClient.Builder builder, ConversationMessageRepository messages, CompanionProfileRepository profiles, MemoryService memory) { this.chat=builder.build(); this.messages=messages; this.profiles=profiles; this.memory=memory; }
 @Transactional public Reply talk(UUID profileId, String text, String context) {
  log.info("Starting companion turn: profileId={}, inputLength={}", profileId, text.length());
  CompanionProfile profile=profiles.findById(profileId).orElseThrow(() -> new NoSuchElementException("Profile not found"));
  messages.save(new ConversationMessage(profileId, MessageRole.USER, text));
  String history=messages.findTop12ByProfileIdOrderByCreatedAtDesc(profileId).stream().filter(m -> m.getRole()!=MessageRole.USER || !m.getContent().equals(text)).limit(10).map(m -> m.getRole()+": "+m.getContent()).collect(Collectors.joining("\n"));
  String memories=memory.relevantTo(profileId, text);
  String system="You are a supportive AI companion, not a therapist or emergency service. Be warm, concise, and curious. " +
   "Your person's name is "+profile.getDisplayName()+". Companion personality: "+profile.getPersonality()+". Interests: "+profile.getInterests()+". " +
   "Use supplied context only when relevant. Known memories (may be irrelevant): "+memories+"\n" +
   " Never claim to have seen or done something you have not. For immediate danger, encourage local emergency services.\nContext:\n"+context+"\nRecent conversation:\n"+history;
  String answer=chat.prompt().system(system).user(text).call().content();
  messages.save(new ConversationMessage(profileId, MessageRole.ASSISTANT, answer)); log.info("Completed companion turn: profileId={}, responseLength={}", profileId, answer.length()); return new Reply(answer);
 }
 public record Reply(String message) {}
}
