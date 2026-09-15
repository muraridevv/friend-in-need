package com.friendinneed.calendar;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CalendarService {
    private final CalendarEventRepository events;

    public CalendarService(CalendarEventRepository events) {
        this.events = events;
    }

    public CalendarEvent create(UUID profileId, String title, Instant startsAt, Instant endsAt) {
        return events.save(new CalendarEvent(profileId, title, startsAt, endsAt));
    }

    public List<CalendarEvent> upcoming(UUID profileId) {
        return events.findByProfileIdAndStartsAtBetweenOrderByStartsAt(profileId, Instant.now(), Instant.now().plus(Duration.ofDays(7)));
    }
}
