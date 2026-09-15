package com.friendinneed.api;

import com.friendinneed.calendar.CalendarEvent;
import com.friendinneed.calendar.CalendarService;
import com.friendinneed.conversation.CompanionService;
import com.friendinneed.conversation.ConversationMessageRepository;
import com.friendinneed.conversation.ConversationMessage;
import com.friendinneed.integration.ContextService;
import com.friendinneed.integration.WeatherService;
import com.friendinneed.integration.NewsService;
import com.friendinneed.memory.MemoryService;
import com.friendinneed.memory.OfflineQueue;
import com.friendinneed.routing.ModelRouter;
import com.friendinneed.profile.CompanionProfile;
import com.friendinneed.profile.CompanionProfileRepository;
import com.friendinneed.security.UserEntity;
import com.friendinneed.security.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CompanionController {
    private static final Logger log = LoggerFactory.getLogger(CompanionController.class);

    private final CompanionService companion;
    private final ContextService context;
    private final WeatherService weather;
    private final CalendarService calendar;
    private final MemoryService memory;
    private final NewsService news;
    private final CompanionProfileRepository profiles;
    private final UserRepository users;
    private final ConversationMessageRepository messages;
    private final ModelRouter modelRouter;
    private final OfflineQueue offlineQueue;

    public CompanionController(
            CompanionService companion,
            ContextService context,
            WeatherService weather,
            CalendarService calendar,
            MemoryService memory,
            NewsService news,
            CompanionProfileRepository profiles,
            UserRepository users,
            ConversationMessageRepository messages,
            ModelRouter modelRouter,
            OfflineQueue offlineQueue) {
        this.companion = companion;
        this.context = context;
        this.weather = weather;
        this.calendar = calendar;
        this.memory = memory;
        this.news = news;
        this.profiles = profiles;
        this.users = users;
        this.messages = messages;
        this.modelRouter = modelRouter;
        this.offlineQueue = offlineQueue;
    }

    @GetMapping("/system/status")
    java.util.Map<String, String> systemStatus() { return modelRouter.status(offlineQueue.depth() > 0); }

    @PostMapping("/profiles")
    @ResponseStatus(HttpStatus.CREATED)
    ProfileView create(@Valid @RequestBody CreateProfile body) {
        return ProfileView.of(profiles.save(new CompanionProfile(currentUser().getId(),
                body.displayName(), body.personality(), body.interests(), body.timezone(), body.location())));
    }

    @GetMapping("/profiles/{id}")
    ProfileView get(@PathVariable UUID id) {
        return ProfileView.of(profile(id));
    }

    @PutMapping("/profiles/{id}")
    ProfileView update(@PathVariable UUID id, @Valid @RequestBody CreateProfile body) {
        CompanionProfile profile = profile(id);
        profile.update(body.displayName(), body.personality(), body.interests(), body.timezone(), body.location());
        return ProfileView.of(profiles.save(profile));
    }

    @DeleteMapping("/profiles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        profiles.delete(profile(id));
    }

    @PutMapping("/profiles/{id}/settings")
    ProfileView settings(@PathVariable UUID id, @Valid @RequestBody SettingsRequest body) {
        CompanionProfile profile = profile(id);
        profile.setProactiveEnabled(body.proactiveEnabled());
        return ProfileView.of(profiles.save(profile));
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<ServerSentEvent<java.util.Map<String, Object>>> streamChat(@RequestParam UUID profileId, @RequestParam String message) {
        CompanionProfile profile = profile(profileId);
        StringBuilder complete = new StringBuilder();
        return companion.talkStreaming(profileId, message, context.relevantContext(profile)).map(token -> {
            complete.append(token); return ServerSentEvent.builder(java.util.Map.<String, Object>of("token", token)).build();
        }).concatWith(Flux.defer(() -> Flux.just(ServerSentEvent.builder(java.util.Map.<String, Object>of("done", true, "fullMessage", complete.toString())).build())));
    }

    @PostMapping("/chat")
    CompanionService.Reply chat(@Valid @RequestBody ChatRequest body) {
        CompanionProfile profile = profile(body.profileId());
        return companion.talk(profile.getId(), body.message(), context.relevantContext(profile));
    }

    @PostMapping("/profiles/{id}/face")
    ProfileView enroll(@PathVariable UUID id, @Valid @RequestBody FaceRequest request) {
        CompanionProfile profile = profile(id);
        log.info("Enrolling face templates: profileId={}, templateCount={}", id, request.descriptor().split("\\|").length);
        profile.enrollFace(request.descriptor());
        return ProfileView.of(profiles.save(profile));
    }

    @PostMapping("/profiles/{id}/face/verify")
    FaceVerification verify(@PathVariable UUID id, @Valid @RequestBody FaceRequest request) {
        CompanionProfile.FaceMatch match = profile(id).faceMatch(request.descriptor());
        log.info("Face verification completed: profileId={}, recognized={}, confidence={}", id, match.recognized(), match.confidence());
        return new FaceVerification(match.recognized(), match.confidence());
    }

    @PostMapping("/profiles/recognize")
    Recognition recognize(@Valid @RequestBody FaceRequest request) {
        return profiles.findByUserIdAndFaceFingerprintIsNotNull(currentUser().getId()).stream()
                .map(candidate -> new RecognizedCandidate(candidate, candidate.faceMatch(request.descriptor())))
                .filter(candidate -> candidate.match().recognized())
                .max(Comparator.comparingLong(candidate -> candidate.match().confidence()))
                .map(candidate -> {
                    log.info("Profile recognized: profileId={}, confidence={}", candidate.profile().getId(), candidate.match().confidence());
                    return new Recognition(true, ProfileView.of(candidate.profile()), candidate.match().confidence());
                })
                .orElseGet(() -> {
                    log.info("No profile matched submitted face templates");
                    return new Recognition(false, null, 0);
                });
    }

    @PostMapping("/profiles/{id}/memories")
    @ResponseStatus(HttpStatus.CREATED)
    void remember(@PathVariable UUID id, @Valid @RequestBody MemoryRequest request) {
        profile(id);
        memory.remember(id, request.content(), request.importance());
    }

    @GetMapping("/profiles/{id}/emotions")
    List<EmotionView> emotions(@PathVariable UUID id, @RequestParam(defaultValue = "20") int limit) {
        profile(id);
        return messages.findTop20ByProfileIdOrderByCreatedAtDesc(id).stream().limit(Math.max(1, Math.min(limit, 20)))
                .map(message -> new EmotionView(message.getRole(), message.getContent(), message.getEmotion(), message.getEmotionConfidence(), message.getCreatedAt())).toList();
    }

    @GetMapping("/profiles/{id}/conversations/export")
    ResponseEntity<List<ConversationMessage>> exportConversations(@PathVariable UUID id) {
        profile(id);
        return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=conversations.json").body(messages.findByProfileIdOrderByCreatedAtAsc(id));
    }

    @DeleteMapping("/profiles/{id}/conversations")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteConversations(@PathVariable UUID id) { profile(id); messages.deleteByProfileId(id); }

    @GetMapping("/profiles/{id}/calendar")
    List<CalendarView> calendar(@PathVariable UUID id) {
        return calendar.upcoming(profile(id).getId()).stream().map(CalendarView::of).toList();
    }

    @PostMapping("/profiles/{id}/calendar")
    @ResponseStatus(HttpStatus.CREATED)
    CalendarView createEvent(@PathVariable UUID id, @Valid @RequestBody EventRequest request) {
        if (request.endsAt() != null && request.endsAt().isBefore(request.startsAt())) {
            throw new IllegalArgumentException("endsAt must be after startsAt");
        }
        profile(id);
        return CalendarView.of(calendar.create(id, request.title(), request.startsAt(), request.endsAt()));
    }

    @GetMapping("/profiles/{id}/briefing")
    Briefing briefing(@PathVariable UUID id) {
        CompanionProfile profile = profile(id);
        return new Briefing(weather.current(profile.getLocation()), calendar.upcoming(id).stream().map(CalendarView::of).toList(), news.headlines(id, "general", 3));
    }

    private CompanionProfile profile(UUID id) {
        CompanionProfile profile = profiles.findById(id).orElseThrow(() -> new NoSuchElementException("Profile not found"));
        if (!currentUser().getId().equals(profile.getUserId())) throw new AccessDeniedException("Profile does not belong to the authenticated user");
        return profile;
    }

    private UserEntity currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) throw new AccessDeniedException("Authentication is required");
        return users.findByUsername(authentication.getName()).orElseThrow(() -> new AccessDeniedException("Authenticated user was not found"));
    }

    record CreateProfile(
            @NotBlank @Size(max = 80) String displayName,
            @NotBlank @Size(max = 2000) String personality,
            @Size(max = 1000) String interests,
            @NotBlank @Size(max = 80) String timezone,
            @NotBlank @Size(max = 120) String location) { }

    record SettingsRequest(boolean proactiveEnabled) { }

    record ChatRequest(@NotNull UUID profileId, @NotBlank @Size(max = 6000) String message) { }

    record FaceRequest(@NotBlank @Size(min = 64, max = 4096) String descriptor) { }

    record FaceVerification(boolean recognized, long confidence) { }

    record Recognition(boolean recognized, ProfileView profile, long confidence) { }

    private record RecognizedCandidate(CompanionProfile profile, CompanionProfile.FaceMatch match) { }

    record MemoryRequest(@NotBlank @Size(max = 1000) String content, @Min(1) @Max(5) int importance) { }

    record EventRequest(@NotBlank @Size(max = 160) String title, @NotNull Instant startsAt, Instant endsAt) { }

    record CalendarView(UUID id, String title, Instant startsAt, Instant endsAt) {
        static CalendarView of(CalendarEvent event) {
            return new CalendarView(event.getId(), event.getTitle(), event.getStartsAt(), event.getEndsAt());
        }
    }

    record EmotionView(com.friendinneed.conversation.MessageRole role, String content, com.friendinneed.emotion.EmotionLabel emotion, Float emotionConfidence, Instant createdAt) { }

    record Briefing(String weather, List<CalendarView> events, List<NewsService.NewsItem> headlines) { }

    record ProfileView(
            UUID id,
            String displayName,
            String personality,
            String interests,
            String timezone,
            String location,
            boolean faceEnrolled,
            boolean proactiveEnabled) {
        static ProfileView of(CompanionProfile profile) {
            return new ProfileView(
                    profile.getId(),
                    profile.getDisplayName(),
                    profile.getPersonality(),
                    profile.getInterests(),
                    profile.getTimezone(),
                    profile.getLocation(),
                    profile.hasFaceEnrollment(),
                    profile.isProactiveEnabled());
        }
    }
}
