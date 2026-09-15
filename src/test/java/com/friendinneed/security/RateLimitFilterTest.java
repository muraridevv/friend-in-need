package com.friendinneed.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitFilterTest {
    private final RateLimitFilter filter = new RateLimitFilter(new ObjectMapper());

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsTwentyFirstChatRequestForUser() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("alex", null));
        AtomicInteger forwarded = new AtomicInteger();
        for (int index = 0; index < 20; index++) {
            MockHttpServletResponse response = call("/api/chat", forwarded);
            assertEquals(200, response.getStatus());
        }
        MockHttpServletResponse rejected = call("/api/chat", forwarded);
        assertEquals(429, rejected.getStatus());
        assertEquals("{\"error\":\"Rate limit exceeded\"}", rejected.getContentAsString());
        assertEquals(20, forwarded.get());
    }

    @Test
    void doesNotLimitOtherApiEndpoints() throws Exception {
        AtomicInteger forwarded = new AtomicInteger();
        assertEquals(200, call("/api/profiles", forwarded).getStatus());
        assertEquals(1, forwarded.get());
    }

    private MockHttpServletResponse call(String path, AtomicInteger forwarded) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> forwarded.incrementAndGet());
        return response;
    }
}
