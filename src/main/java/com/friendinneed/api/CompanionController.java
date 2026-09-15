package com.friendinneed.api;
import com.friendinneed.conversation.CompanionService; import com.friendinneed.integration.ContextService; import com.friendinneed.profile.*;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api") public class CompanionController {
 private final CompanionService companion; private final ContextService context; private final CompanionProfileRepository profiles;
 public CompanionController(CompanionService companion, ContextService context, CompanionProfileRepository profiles){this.companion=companion;this.context=context;this.profiles=profiles;}
 @PostMapping("/profiles") @ResponseStatus(HttpStatus.CREATED) ProfileView create(@Valid @RequestBody CreateProfile body){var p=profiles.save(new CompanionProfile(body.displayName(),body.personality(),body.interests(),body.timezone()));return ProfileView.of(p);}
 @GetMapping("/profiles/{id}") ProfileView get(@PathVariable UUID id){return ProfileView.of(profile(id));}
 @PostMapping("/chat") CompanionService.Reply chat(@Valid @RequestBody ChatRequest body){return companion.talk(body.profileId(),body.message(),context.relevantContext(body.message()));}
 @PostMapping("/profiles/{id}/face") ProfileView enroll(@PathVariable UUID id,@Valid @RequestBody FaceRequest request){var p=profile(id);p.enrollFace(request.fingerprint());return ProfileView.of(profiles.save(p));}
 @PostMapping("/profiles/{id}/face/verify") Map<String,Boolean> verify(@PathVariable UUID id,@Valid @RequestBody FaceRequest request){return Map.of("recognized",profile(id).faceMatches(request.fingerprint()));}
 private CompanionProfile profile(UUID id){return profiles.findById(id).orElseThrow(()->new NoSuchElementException("Profile not found"));}
 record CreateProfile(@NotBlank @Size(max=80) String displayName,@NotBlank @Size(max=2000) String personality,@Size(max=1000) String interests,@NotBlank String timezone){}
 record ChatRequest(@NotNull UUID profileId,@NotBlank @Size(max=6000) String message){}
 record FaceRequest(@NotBlank @Size(max=128) String fingerprint){}
 record ProfileView(UUID id,String displayName,String personality,String interests,String timezone,boolean faceEnrolled){static ProfileView of(CompanionProfile p){return new ProfileView(p.getId(),p.getDisplayName(),p.getPersonality(),p.getInterests(),p.getTimezone(),p.hasFaceEnrollment());}}
}
