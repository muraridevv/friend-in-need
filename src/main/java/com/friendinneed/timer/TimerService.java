package com.friendinneed.timer;

import com.friendinneed.proactive.NotificationPublisher;
import com.friendinneed.proactive.ProactiveMessage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TimerService {
    private final TimerRepository timers;
    private final NotificationPublisher notifications;
    private final Clock clock;

    public TimerService(TimerRepository t, NotificationPublisher n, Clock c) {
        timers = t;
        notifications = n;
        clock = c;
    }

    public List<Timer> list(UUID p) {
        return timers.findByProfileIdOrderByTriggerAt(p);
    }

    public Timer create(UUID p, String label, Instant at) {
        return timers.save(new Timer(p, label, at));
    }

    public void delete(UUID p, UUID id) {
        Timer t = timers.findById(id).filter(x -> x.getProfileId().equals(p)).orElseThrow();
        timers.delete(t);
    }

    @Scheduled(fixedDelay = 15000)
    public void fireDue() {
        for (Timer t : timers.findByFiredFalseAndTriggerAtLessThanEqual(clock.instant())) {
            t.markFired();
            timers.save(t);
            notifications.publish(new ProactiveMessage(t.getProfileId(), "Timer: " + t.getLabel(), clock.instant()));
        }
    }

    public Optional<Timer> createFromNaturalLanguage(UUID p, String text) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)remind me in (\\d+) (minute|minutes|hour|hours) to (.+)").matcher(text);
        if (!m.matches()) return Optional.empty();
        long amount = Long.parseLong(m.group(1));
        Duration d = m.group(2).startsWith("hour") ? Duration.ofHours(amount) : Duration.ofMinutes(amount);
        return Optional.of(create(p, m.group(3), clock.instant().plus(d)));
    }
}
