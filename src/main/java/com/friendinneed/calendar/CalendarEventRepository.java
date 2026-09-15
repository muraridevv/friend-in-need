package com.friendinneed.calendar;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {
    List<CalendarEvent> findByProfileIdAndStartsAtBetweenOrderByStartsAt(UUID profileId, Instant start, Instant end);

    List<CalendarEvent> findByStartsAtBetweenAndRemindedFalse(Instant start, Instant end);
}
