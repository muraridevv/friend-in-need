package com.friendinneed.calendar;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
public class CalendarEvent {
    @Id private UUID id;
    private UUID profileId;
    private String title;
    private Instant startsAt;
    private Instant endsAt;
    private Instant createdAt;
    protected CalendarEvent() { }
    public CalendarEvent(UUID profileId, String title, Instant startsAt, Instant endsAt) { this.id=UUID.randomUUID(); this.profileId=profileId; this.title=title; this.startsAt=startsAt; this.endsAt=endsAt; this.createdAt=Instant.now(); }
    public UUID getId(){ return id; } public String getTitle(){ return title; } public Instant getStartsAt(){ return startsAt; } public Instant getEndsAt(){ return endsAt; }
}
