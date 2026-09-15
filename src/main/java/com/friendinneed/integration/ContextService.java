package com.friendinneed.integration;

import com.friendinneed.calendar.CalendarService;
import com.friendinneed.profile.CompanionProfile;
import org.springframework.stereotype.Service;
import java.time.*;

@Service
public class ContextService {
    private final WeatherService weather; private final CalendarService calendar;
    public ContextService(WeatherService weather, CalendarService calendar) { this.weather=weather; this.calendar=calendar; }
    public String relevantContext(CompanionProfile profile) {
        String appointments = calendar.upcoming(profile.getId()).stream().limit(3).map(event -> event.getTitle()+" at "+event.getStartsAt()).reduce((a,b)->a+"; "+b).orElse("No upcoming calendar events.");
        return "Local time: "+ZonedDateTime.now(ZoneId.of(profile.getTimezone()))+". "+weather.current(profile.getLocation())+" Upcoming calendar: "+appointments;
    }
}
