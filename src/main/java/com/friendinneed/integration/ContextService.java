package com.friendinneed.integration;

import com.friendinneed.calendar.CalendarService;
import com.friendinneed.profile.CompanionProfile;
import org.springframework.stereotype.Service;
import java.time.*;

@Service
public class ContextService {
    private final WeatherService weather; private final CalendarService calendar; private final NewsService news;
    public ContextService(WeatherService weather, CalendarService calendar, NewsService news) { this.weather=weather; this.calendar=calendar; this.news=news; }
    public String relevantContext(CompanionProfile profile) {
        String appointments = calendar.upcoming(profile.getId()).stream().limit(3).map(event -> event.getTitle()+" at "+event.getStartsAt()).reduce((a,b)->a+"; "+b).orElse("No upcoming calendar events.");
        ZoneId timezone;
        try { timezone = ZoneId.of(profile.getTimezone()); } catch (DateTimeException ignored) { timezone = ZoneId.of("UTC"); }
        String headlines = news.headlines(profile.getId(), "general", 3).stream().map(NewsService.NewsItem::title).reduce((a,b) -> a + "; " + b).orElse("No news headlines.");
        return "Local time: "+ZonedDateTime.now(timezone)+". "+weather.current(profile.getLocation())+" Upcoming calendar: "+appointments+" News: "+headlines;
    }
}
