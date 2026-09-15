package com.friendinneed.conversation;

import com.friendinneed.memory.MemoryService;
import com.friendinneed.consent.*;
import com.friendinneed.smarthome.*;
import com.friendinneed.routine.RoutineService;
import com.friendinneed.timer.TimerService;
import com.friendinneed.routing.ModelChoice;
import com.friendinneed.routing.ModelRouter;
import com.friendinneed.routing.OllamaAdapter;
import com.friendinneed.profile.CompanionProfile;
import com.friendinneed.profile.CompanionProfileRepository;
import com.friendinneed.token.TokenBudgetService;
import com.friendinneed.emotion.EmotionService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Flux;

@Service
public class CompanionService {
    private static final Logger log = LoggerFactory.getLogger(CompanionService.class);
    private static final String SYSTEM_PROMPT_TEMPLATE = "classpath:templates/system-prompt.mustache";
    private static final AtomicLong LOCAL_TURNS = new AtomicLong();
    private static final Pattern MEMORY_WORTHY = Pattern.compile("(?i)\\b(i (like|love|prefer|hate|have)|my (name|dog|cat|birthday)|i(?:'m| am) (named|allergic)|on \\d{4}-\\d{2}-\\d{2})\\b");
    private final ChatClient chat;
    private final ConversationMessageRepository messages;
    private final CompanionProfileRepository profiles;
    private final MemoryService memory;
    private final ResourceLoader resourceLoader;
    private final TokenBudgetService tokenBudget;
    private final EmotionService emotions;
    private final ModelRouter modelRouter;
    private final OllamaAdapter ollama;
    private final ConsentService consent;
    private final SmartHomeAdapter home;
    private final RoutineService routines;
    private final TimerService timers;

    public CompanionService(ChatClient.Builder builder, ConversationMessageRepository messages,
                            CompanionProfileRepository profiles, MemoryService memory, ResourceLoader resourceLoader,
                            TokenBudgetService tokenBudget, EmotionService emotions, ModelRouter modelRouter, OllamaAdapter ollama, ConsentService consent, SmartHomeAdapter home, RoutineService routines, TimerService timers) {
        this.chat = builder.build();
        this.messages = messages;
        this.profiles = profiles;
        this.memory = memory;
        this.resourceLoader = resourceLoader;
        this.tokenBudget = tokenBudget;
        this.emotions = emotions;
        this.modelRouter = modelRouter;
        this.ollama = ollama;
        this.consent = consent;
        this.home = home;
        this.routines = routines;
        this.timers = timers;
    }

    public Reply talk(UUID profileId, String text, String context) {
        log.info("Starting companion turn: profileId={}, inputLength={}", profileId, text.length());
        if (consent.isEnabled(profileId, IntegrationType.SMART_HOME)) {
            if (routines.executeForPhrase(profileId, text)) return new Reply("Done, I ran your routine.");
            java.util.Optional<String> deviceReply = deviceCommand(text);
            if (deviceReply.isPresent()) return new Reply(deviceReply.get());
        }
        java.util.Optional<com.friendinneed.timer.Timer> timer = timers.createFromNaturalLanguage(profileId, text);
        if (timer.isPresent()) return new Reply("Done, I’ll remind you to " + timer.get().getLabel() + ".");
        CompanionProfile profile = profiles.findById(profileId)
                .orElseThrow(() -> new NoSuchElementException("Profile not found"));
        ConversationMessage userMessage = messages.save(new ConversationMessage(profileId, MessageRole.USER, text));
        EmotionService.EmotionResult emotion = emotions.detectFromText(text);
        userMessage.setEmotion(emotion.emotion(), emotion.confidence());
        List<ConversationMessage> recentMessages = new ArrayList<>(messages.findTop12ByProfileIdOrderByCreatedAtDesc(profileId));
        Collections.reverse(recentMessages);
        String history = recentMessages.stream().filter(message -> !message.getId().equals(userMessage.getId())).limit(10)
                .map(message -> message.getRole() + ": " + message.getContent()).collect(Collectors.joining("\n"));
        String memories = memory.relevantTo(profileId, text);
        String system = renderSystemPrompt(Map.of("displayName", profile.getDisplayName(), "personality", profile.getPersonality(),
                "interests", profile.getInterests(), "memories", memories, "context", context + " The user current emotional state appears to be: " + emotion.emotion() + " (confidence: " + emotion.confidence() + "). Adjust your tone accordingly.", "history", history));
        String prompt = tokenBudget.trimToFit(system, "", "", 0);
        ModelChoice choice = modelRouter.routeChat();
        log.info("Serving companion turn with {} model: profileId={}", choice, profileId);
        String answer = choice == ModelChoice.LOCAL ? ollama.talk(prompt, text) : chat.prompt().system(prompt).user(text).call().content();
        if (choice == ModelChoice.LOCAL && LOCAL_TURNS.incrementAndGet() % 5 == 1) {
            answer = "I’m running on my local brain right now, so I might be briefer than usual. " + answer;
        }
        messages.save(new ConversationMessage(profileId, MessageRole.ASSISTANT, answer));
        rememberIfUseful(profileId, text);
        log.info("Completed companion turn: profileId={}, responseLength={}", profileId, answer.length());
        return new Reply(answer);
    }

    public Flux<String> talkStreaming(UUID profileId, String text, String context) {
        CompanionProfile profile = profiles.findById(profileId).orElseThrow(() -> new NoSuchElementException("Profile not found"));
        ConversationMessage user = messages.save(new ConversationMessage(profileId, MessageRole.USER, text));
        EmotionService.EmotionResult emotion = emotions.detectFromText(text);
        user.setEmotion(emotion.emotion(), emotion.confidence());
        List<ConversationMessage> recent = new ArrayList<>(messages.findTop12ByProfileIdOrderByCreatedAtDesc(profileId));
        Collections.reverse(recent);
        String history = recent.stream().filter(m -> !m.getId().equals(user.getId())).limit(10).map(m -> m.getRole() + ": " + m.getContent()).collect(Collectors.joining("\n"));
        String memories = memory.relevantTo(profileId, text);
        String system = renderSystemPrompt(Map.of("displayName", profile.getDisplayName(), "personality", profile.getPersonality(), "interests", profile.getInterests(), "memories", memories, "history", history, "context", context + " User emotion: " + emotion.emotion()));
        String prompt = tokenBudget.trimToFit(system, "", "", 0);
        ModelChoice choice = modelRouter.routeChat();
        StringBuilder full = new StringBuilder();
        Flux<String> response = choice == ModelChoice.LOCAL ? ollama.talkStreaming(prompt, text) : chat.prompt().system(prompt).user(text).stream().content();
        return response.doOnNext(full::append).doOnComplete(() -> {
            messages.save(new ConversationMessage(profileId, MessageRole.ASSISTANT, full.toString()));
            rememberIfUseful(profileId, text);
        });
    }


    private java.util.Optional<String> deviceCommand(String text) {
        String normalized = text.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("temperature")) {
            DeviceState thermostat = home.getDevice("climate.thermostat");
            return java.util.Optional.of("It’s " + thermostat.state() + "°C in here.");
        }
        if (normalized.contains("turn off") && normalized.contains("light")) {
            home.sendCommand("light.living_room", "turn_off", Map.of());
            return java.util.Optional.of("Done, I’ve turned off the living room light.");
        }
        if (normalized.contains("turn on") && normalized.contains("light")) {
            home.sendCommand("light.living_room", "turn_on", Map.of());
            return java.util.Optional.of("Done, I’ve turned on the living room light.");
        }
        return java.util.Optional.empty();
    }

    private void rememberIfUseful(UUID profileId, String userText) {
        if (MEMORY_WORTHY.matcher(userText).find()) {
            memory.remember(profileId, userText, 3);
            log.info("Automatically stored a memory from conversation: profileId={}", profileId);
        }
    }

    private String renderSystemPrompt(Map<String, String> variables) {
        try {
            String template = StreamUtils.copyToString(resourceLoader.getResource(SYSTEM_PROMPT_TEMPLATE).getInputStream(), StandardCharsets.UTF_8);
            String rendered = template;
            for (Map.Entry<String, String> variable : variables.entrySet())
                rendered = rendered.replace("{{" + variable.getKey() + "}}", variable.getValue());
            return rendered;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load system prompt template", exception);
        }
    }

    public record Reply(String message) {
    }
}
