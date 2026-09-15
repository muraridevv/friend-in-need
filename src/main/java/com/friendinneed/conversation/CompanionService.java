package com.friendinneed.conversation;

import com.friendinneed.memory.MemoryService;
import com.friendinneed.profile.CompanionProfile;
import com.friendinneed.profile.CompanionProfileRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

@Service
public class CompanionService {
    private static final Logger log = LoggerFactory.getLogger(CompanionService.class);
    private static final String SYSTEM_PROMPT_TEMPLATE = "classpath:templates/system-prompt.mustache";

    private final ChatClient chat;
    private final ConversationMessageRepository messages;
    private final CompanionProfileRepository profiles;
    private final MemoryService memory;
    private final ResourceLoader resourceLoader;

    public CompanionService(
            ChatClient.Builder builder,
            ConversationMessageRepository messages,
            CompanionProfileRepository profiles,
            MemoryService memory,
            ResourceLoader resourceLoader) {
        this.chat = builder.build();
        this.messages = messages;
        this.profiles = profiles;
        this.memory = memory;
        this.resourceLoader = resourceLoader;
    }

    @Transactional
    public Reply talk(UUID profileId, String text, String context) {
        log.info("Starting companion turn: profileId={}, inputLength={}", profileId, text.length());
        CompanionProfile profile = profiles.findById(profileId)
                .orElseThrow(() -> new NoSuchElementException("Profile not found"));
        ConversationMessage userMessage = messages.save(new ConversationMessage(profileId, MessageRole.USER, text));
        List<ConversationMessage> recentMessages = new ArrayList<>(messages.findTop12ByProfileIdOrderByCreatedAtDesc(profileId));
        Collections.reverse(recentMessages);
        String history = recentMessages.stream()
                .filter(message -> !message.getId().equals(userMessage.getId()))
                .limit(10)
                .map(message -> message.getRole() + ": " + message.getContent())
                .collect(Collectors.joining("\n"));
        String memories = memory.relevantTo(profileId, text);
        String system = renderSystemPrompt(Map.of(
                "displayName", profile.getDisplayName(),
                "personality", profile.getPersonality(),
                "interests", profile.getInterests(),
                "memories", memories,
                "context", context,
                "history", history));
        String answer = chat.prompt().system(system).user(text).call().content();
        messages.save(new ConversationMessage(profileId, MessageRole.ASSISTANT, answer));
        log.info("Completed companion turn: profileId={}, responseLength={}", profileId, answer.length());
        return new Reply(answer);
    }

    private String renderSystemPrompt(Map<String, String> variables) {
        try {
            String template = StreamUtils.copyToString(
                    resourceLoader.getResource(SYSTEM_PROMPT_TEMPLATE).getInputStream(), StandardCharsets.UTF_8);
            String rendered = template;
            for (Map.Entry<String, String> variable : variables.entrySet()) {
                rendered = rendered.replace("{{" + variable.getKey() + "}}", variable.getValue());
            }
            return rendered;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load system prompt template", exception);
        }
    }

    public record Reply(String message) { }
}
