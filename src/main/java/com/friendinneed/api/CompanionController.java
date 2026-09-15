package com.friendinneed.api;

import com.friendinneed.calendar.CalendarEvent;
import com.friendinneed.calendar.CalendarService;
import com.friendinneed.conversation.CompanionService;
import com.friendinneed.integration.ContextService;
import com.friendinneed.integration.WeatherService;
import com.friendinneed.memory.MemoryService;
import com.friendinneed.profile.CompanionProfile;
import com.friendinneed.profile.CompanionProfileRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api")
public class CompanionController {
    private static final Logger log = LoggerFactory.getLogger(CompanionController.class);
    private final CompanionService companion; private final ContextService context; private final WeatherService weather;
    private final CalendarService calendar; private final MemoryService memory; private final CompanionProfileRepository profiles;
    public CompanionController(CompanionService companion, ContextService context, WeatherService weather, CalendarService calendar, MemoryService memory, CompanionProfileRepository profiles) {
        this.companion=companion; this.context=context; this.weather=weather; this.calendar=calendar; this.memory=memory; this.profiles=profiles;
    }
    @PostMapping("/profiles") @ResponseStatus(HttpStatus.CREATED)
    ProfileView create(@Valid @RequestBody CreateProfile body) { return ProfileView.of(profiles.save(new CompanionProfile(body.displayName(),body.personality(),body.interests(),body.timezone(),body.location()))); }
    @GetMapping("/profiles/{id}") ProfileView get(@PathVariable UUID id) { return ProfileView.of(profile(id)); }
    @PostMapping("/chat") CompanionService.Reply chat(@Valid @RequestBody ChatRequest body) { CompanionProfile p=profile(body.profileId()); return companion.talk(p.getId(),body.message(),context.relevantContext(p)); }
    @PostMapping("/profiles/{id}/face") ProfileView enroll(@PathVariable UUID id,@Valid @RequestBody FaceRequest request) { var p=profile(id); log.info("Enrolling face templates: profileId={}, templateCount={}", id, request.descriptor().split("\\|").length); p.enrollFace(request.descriptor()); return ProfileView.of(profiles.save(p)); }
    @PostMapping("/profiles/{id}/face/verify") FaceVerification verify(@PathVariable UUID id,@Valid @RequestBody FaceRequest request) { var match=profile(id).faceMatch(request.descriptor()); log.info("Face verification completed: profileId={}, recognized={}, confidence={}", id, match.recognized(), match.confidence()); return new FaceVerification(match.recognized(), match.confidence()); }
    @PostMapping("/profiles/recognize") Recognition recognize(@Valid @RequestBody FaceRequest request) {
        return profiles.findAll().stream()
                .map(candidate -> new RecognizedCandidate(candidate, candidate.faceMatch(request.descriptor())))
                .filter(candidate -> candidate.match().recognized())
                .max(Comparator.comparingLong(candidate -> candidate.match().confidence()))
                .map(candidate -> { log.info("Profile recognized: profileId={}, confidence={}", candidate.profile().getId(), candidate.match().confidence()); return new Recognition(true, ProfileView.of(candidate.profile()), candidate.match().confidence()); })
                .orElseGet(() -> { log.info("No profile matched submitted face templates"); return new Recognition(false, null, 0); });
    }
    @PostMapping("/profiles/{id}/memories") @ResponseStatus(HttpStatus.CREATED)
    void remember(@PathVariable UUID id, @Valid @RequestBody MemoryRequest request) { profile(id); memory.remember(id,request.content(),request.importance()); }
    @GetMapping("/profiles/{id}/calendar") List<CalendarView> calendar(@PathVariable UUID id) { return calendar.upcoming(profile(id).getId()).stream().map(CalendarView::of).toList(); }
    @PostMapping("/profiles/{id}/calendar") @ResponseStatus(HttpStatus.CREATED)
    CalendarView createEvent(@PathVariable UUID id,@Valid @RequestBody EventRequest request) { profile(id); return CalendarView.of(calendar.create(id,request.title(),request.startsAt(),request.endsAt())); }
    @GetMapping("/profiles/{id}/briefing") Briefing briefing(@PathVariable UUID id) { var p=profile(id); return new Briefing(weather.current(p.getLocation()),calendar.upcoming(id).stream().map(CalendarView::of).toList()); }
    private CompanionProfile profile(UUID id) { return profiles.findById(id).orElseThrow(()->new NoSuchElementException("Profile not found")); }
    record CreateProfile(@NotBlank @Size(max=80) String displayName,@NotBlank @Size(max=2000) String personality,@Size(max=1000) String interests,@NotBlank @Size(max=80) String timezone,@NotBlank @Size(max=120) String location) { }
    record ChatRequest(@NotNull UUID profileId,@NotBlank @Size(max=6000) String message) { }
    record FaceRequest(@NotBlank @Size(min=64, max=4096) String descriptor) { }
    record FaceVerification(boolean recognized, long confidence) { }
    record Recognition(boolean recognized, ProfileView profile, long confidence) { }
    private record RecognizedCandidate(CompanionProfile profile, CompanionProfile.FaceMatch match) { }
    record MemoryRequest(@NotBlank @Size(max=1000) String content,@Min(1) @Max(5) int importance) { }
    record EventRequest(@NotBlank @Size(max=160) String title,@NotNull Instant startsAt, Instant endsAt) { }
    record CalendarView(UUID id,String title,Instant startsAt,Instant endsAt) { static CalendarView of(CalendarEvent e) { return new CalendarView(e.getId(),e.getTitle(),e.getStartsAt(),e.getEndsAt()); } }
    record Briefing(String weather,List<CalendarView> events) { }
    record ProfileView(UUID id,String displayName,String personality,String interests,String timezone,String location,boolean faceEnrolled) { static ProfileView of(CompanionProfile p) { return new ProfileView(p.getId(),p.getDisplayName(),p.getPersonality(),p.getInterests(),p.getTimezone(),p.getLocation(),p.hasFaceEnrollment()); } }
}
