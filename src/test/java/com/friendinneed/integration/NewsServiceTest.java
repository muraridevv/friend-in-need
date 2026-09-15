package com.friendinneed.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.friendinneed.consent.ConsentService;
import com.friendinneed.consent.IntegrationType;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class NewsServiceTest {
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void parsesHeadlinesFromNewsApiResponse() throws Exception {
        ConsentService consents = mock(ConsentService.class); UUID profileId = UUID.randomUUID();
        when(consents.isEnabled(profileId, IntegrationType.NEWS)).thenReturn(true);
        RestClient.Builder builder = mock(RestClient.Builder.class); RestClient client = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec request = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headers = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec response = mock(RestClient.ResponseSpec.class);
        when(builder.baseUrl(anyString())).thenReturn(builder); when(builder.build()).thenReturn(client);
        when(client.get()).thenReturn(request); when(request.uri(any(java.util.function.Function.class))).thenReturn(headers); when(headers.retrieve()).thenReturn(response);
        when(response.body(com.fasterxml.jackson.databind.JsonNode.class)).thenReturn(new ObjectMapper().readTree("{\"articles\":[{\"title\":\"Hello\",\"description\":\"World\",\"url\":\"https://example.test\",\"publishedAt\":\"2026-01-01T00:00:00Z\"}]}"));
        NewsService service = new NewsService(consents, builder, "https://news.test", "key");
        NewsService.NewsItem item = service.headlines(profileId, "general", 3).getFirst();
        assertEquals("Hello", item.title()); assertEquals("https://example.test", item.url());
    }
}
