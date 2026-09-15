package com.friendinneed.proactive;

import com.friendinneed.calendar.CalendarEvent;
import com.friendinneed.calendar.CalendarEventRepository;
import com.friendinneed.conversation.ConversationMessageRepository;
import com.friendinneed.integration.WeatherService;
import com.friendinneed.profile.CompanionProfile;
import com.friendinneed.profile.CompanionProfileRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import com.friendinneed.routing.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProactiveCheckIn {
    private final CompanionProfileRepository profiles;
    private final CalendarEventRepository events;
    private final ConversationMessageRepository conversations;
    private final WeatherService weather;
    private final ProactiveMessageRepository messages;
    private final NotificationPublisher notifications;
    private final ChatClient chat;
    private final Clock clock; private final ModelRouter router; private final OllamaAdapter ollama;

    public ProactiveCheckIn(CompanionProfileRepository profiles, CalendarEventRepository events,
            ConversationMessageRepository conversations, WeatherService weather, ProactiveMessageRepository messages,
            NotificationPublisher notifications, ChatClient.Builder chatBuilder, Clock clock, ModelRouter router, OllamaAdapter ollama) {
        this.profiles = profiles; this.events = events; this.conversations = conversations; this.weather = weather;
        this.messages = messages; this.notifications = notifications; this.chat = chatBuilder.build(); this.clock = clock; this.router=router; this.ollama=ollama;
    }

    @Scheduled(cron = "${companion.proactive-cron}")
    @Transactional
    public void sendScheduledCheckIns() {
        profiles.findByProactiveEnabledTrue().forEach(this::sendCheckInIfAppropriate);
    }

    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void sendCalendarReminders() {
        Instant now = clock.instant();
        events.findByStartsAtBetweenAndRemindedFalse(now, now.plusSeconds(30 * 60)).forEach(event -> {
            ProactiveMessage message = messages.save(new ProactiveMessage(event.getProfileId(),
                    "Reminder: " + event.getTitle() + " starts soon.", now));
            event.markReminded();
            if (notifications.publish(message)) message.markDelivered(now);
        });
    }

    void sendCheckInIfAppropriate(CompanionProfile profile) {
        ZonedDateTime localTime = ZonedDateTime.now(clock).withZoneSameInstant(zone(profile));
        if (localTime.getHour() < 8 || localTime.getHour() >= 22) return;
        Instant now = clock.instant();
        String context = contextFor(profile, now);
        String prompt = "Generate a single warm, brief check-in message for " + profile.getDisplayName()
                + ". Context: " + context + ". Do not ask more than one question.";
        String content = router.routeChat() == ModelChoice.LOCAL ? ollama.talk(prompt, "Write the check-in now.") : chat.prompt().system(prompt).user("Write the check-in now.").call().content();
        ProactiveMessage message = messages.save(new ProactiveMessage(profile.getId(), content, now));
        if (notifications.publish(message)) message.markDelivered(now);
    }

    private String contextFor(CompanionProfile profile, Instant now) {
        List<CalendarEvent> upcoming = events.findByProfileIdAndStartsAtBetweenOrderByStartsAt(profile.getId(), now, now.plusSeconds(7 * 86_400));
        String eventSummary = upcoming.stream().limit(2).map(CalendarEvent::getTitle).reduce((a, b) -> a + ", " + b).orElse("no upcoming events");
        String emotionTrend = conversations.findTop12ByProfileIdOrderByCreatedAtDesc(profile.getId()).isEmpty() ? "no recent conversation" : "recent conversation available";
        return weather.current(profile.getLocation()) + "; upcoming: " + eventSummary + "; emotion trend: " + emotionTrend;
    }

    private ZoneId zone(CompanionProfile profile) {
        try { return ZoneId.of(profile.getTimezone()); } catch (RuntimeException exception) { return ZoneId.of("UTC"); }
    }
}
