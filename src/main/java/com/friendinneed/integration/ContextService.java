package com.friendinneed.integration;

import com.friendinneed.calendar.CalendarService;
import com.friendinneed.profile.CompanionProfile;
import com.friendinneed.consent.*;
import com.friendinneed.smarthome.*;
import com.friendinneed.timer.TimerService;
import org.springframework.stereotype.Service;
import java.time.*;

@Service
public class ContextService {
    private final WeatherService weather; private final CalendarService calendar; private final NewsService news; private final ConsentService consent; private final SmartHomeAdapter home; private final TimerService timers;
    public ContextService(WeatherService weather, CalendarService calendar, NewsService news, ConsentService consent, SmartHomeAdapter home, TimerService timers) { this.weather=weather; this.calendar=calendar; this.news=news; this.consent=consent; this.home=home; this.timers=timers; }
    public String relevantContext(CompanionProfile profile) {
        String appointments = calendar.upcoming(profile.getId()).stream().limit(3).map(event -> event.getTitle()+" at "+event.getStartsAt()).reduce((a,b)->a+"; "+b).orElse("No upcoming calendar events.");
        ZoneId timezone;
        try { timezone = ZoneId.of(profile.getTimezone()); } catch (DateTimeException ignored) { timezone = ZoneId.of("UTC"); }
        String headlines = news.headlines(profile.getId(), "general", 3).stream().map(NewsService.NewsItem::title).reduce((a,b) -> a + "; " + b).orElse("No news headlines.");
        String homeContext = ""; if (consent.isEnabled(profile.getId(), IntegrationType.SMART_HOME)) { homeContext = " Smart-home devices: " + home.listDevices() + ". Active timers: " + timers.list(profile.getId()).stream().filter(timer -> !timer.isFired()).map(timer -> timer.getLabel()+" at "+timer.getTriggerAt()).reduce((a,b)->a+"; "+b).orElse("none") + "."; } return "Local time: "+ZonedDateTime.now(timezone)+". "+weather.current(profile.getLocation())+" Upcoming calendar: "+appointments+" News: "+headlines+homeContext;
    }
}
