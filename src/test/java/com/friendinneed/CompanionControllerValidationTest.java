package com.friendinneed;

import com.friendinneed.api.CompanionController;
import com.friendinneed.calendar.CalendarService;
import com.friendinneed.conversation.CompanionService;
import com.friendinneed.integration.ContextService;
import com.friendinneed.integration.WeatherService;
import com.friendinneed.integration.NewsService;
import com.friendinneed.memory.MemoryService;
import com.friendinneed.profile.CompanionProfileRepository;
import com.friendinneed.security.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CompanionController.class)
@AutoConfigureMockMvc(addFilters = false)
class CompanionControllerValidationTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean private CompanionService companion;
    @MockBean private ContextService context;
    @MockBean private WeatherService weather;
    @MockBean private CalendarService calendar;
    @MockBean private MemoryService memory;
    @MockBean private NewsService news;
    @MockBean private CompanionProfileRepository profiles;
    @MockBean private UserRepository users;

    @Test
    void testCreateProfileBlankName() throws Exception {
        mockMvc.perform(post("/api/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"","personality":"warm","interests":"","timezone":"UTC","location":"New York"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testChatBlankMessage() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"profileId":"%s","message":""}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testCalendarEventEndBeforeStart() throws Exception {
        mockMvc.perform(post("/api/profiles/{id}/calendar", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Past event","startsAt":"2026-01-02T10:00:00Z","endsAt":"2026-01-01T10:00:00Z"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("endsAt must be after startsAt"));
    }
}
