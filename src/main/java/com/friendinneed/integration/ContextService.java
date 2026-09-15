package com.friendinneed.integration;
import org.springframework.stereotype.Service; import java.time.*;
/** Integration boundary: replace adapters below with OAuth calendar/weather/news providers. */
@Service public class ContextService {
 public String relevantContext(String requested) {
  return "Current local time (UTC): "+Instant.now()+". Calendar, weather, and news providers are opt-in and not connected yet. Do not invent their data.";
 }
}
