package com.friendinneed.timer;

import com.friendinneed.proactive.NotificationPublisher;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

class TimerServiceTest {
    @Test
    void firesDueTimerUsingInjectedClock() {
        TimerRepository repo = mock(TimerRepository.class);
        NotificationPublisher notifications = mock(NotificationPublisher.class);
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);
        Timer timer = new Timer(UUID.randomUUID(), "oven", clock.instant());
        when(repo.findByFiredFalseAndTriggerAtLessThanEqual(clock.instant())).thenReturn(List.of(timer));
        new TimerService(repo, notifications, clock).fireDue();
        verify(repo).save(timer);
        verify(notifications).publish(any());
    }
}
