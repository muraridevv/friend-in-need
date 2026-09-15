package com.friendinneed.proactive;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.friendinneed.calendar.CalendarEvent;
import com.friendinneed.calendar.CalendarEventRepository;
import com.friendinneed.conversation.ConversationMessageRepository;
import com.friendinneed.integration.WeatherService;
import com.friendinneed.profile.CompanionProfile;
import com.friendinneed.profile.CompanionProfileRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import static org.mockito.Mockito.mock;

class ProactiveCheckInTest {
    @Test
    void skipsCheckInDuringQuietHours() {
        ProactiveMessageRepository messages = mock(ProactiveMessageRepository.class);
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(mock(ChatClient.class));
        ProactiveCheckIn checkIn = service(Clock.fixed(Instant.parse("2026-01-01T07:00:00Z"), ZoneOffset.UTC),
                mock(CalendarEventRepository.class), messages, builder);
        CompanionProfile profile = new CompanionProfile(null, "Sam", "warm", "music", "UTC", "New York");

        checkIn.sendCheckInIfAppropriate(profile);
        verify(messages, never()).save(any());
    }

    @Test
    void remindsEventsStartingWithinThirtyMinutesOnly() {
        Instant now = Instant.parse("2026-01-01T12:00:00Z");
        CalendarEventRepository events = mock(CalendarEventRepository.class);
        ProactiveMessageRepository messages = mock(ProactiveMessageRepository.class);
        CalendarEvent soon = new CalendarEvent(java.util.UUID.randomUUID(), "Call Alex", now.plusSeconds(20 * 60), null);
        when(events.findByStartsAtBetweenAndRemindedFalse(eq(now), eq(now.plusSeconds(30 * 60)))).thenReturn(List.of(soon));
        when(messages.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ProactiveCheckIn checkIn = service(Clock.fixed(now, ZoneOffset.UTC), events, messages, mock(ChatClient.Builder.class));

        checkIn.sendCalendarReminders();
        verify(messages).save(any(ProactiveMessage.class));
        verify(events).findByStartsAtBetweenAndRemindedFalse(now, now.plusSeconds(30 * 60));
    }

    @Test
    void doesNotRemindEventOutsideThirtyMinuteWindow() {
        Instant now = Instant.parse("2026-01-01T12:00:00Z");
        CalendarEventRepository events = mock(CalendarEventRepository.class);
        ProactiveMessageRepository messages = mock(ProactiveMessageRepository.class);
        when(events.findByStartsAtBetweenAndRemindedFalse(eq(now), eq(now.plusSeconds(30 * 60)))).thenReturn(List.of());
        ProactiveCheckIn checkIn = service(Clock.fixed(now, ZoneOffset.UTC), events, messages, mock(ChatClient.Builder.class));

        checkIn.sendCalendarReminders();
        verify(messages, never()).save(any());
    }

    private ProactiveCheckIn service(Clock clock, CalendarEventRepository events, ProactiveMessageRepository messages, ChatClient.Builder builder) {
        return new ProactiveCheckIn(mock(CompanionProfileRepository.class), events, mock(ConversationMessageRepository.class),
                mock(WeatherService.class), messages, mock(NotificationPublisher.class), builder, clock);
    }
}
