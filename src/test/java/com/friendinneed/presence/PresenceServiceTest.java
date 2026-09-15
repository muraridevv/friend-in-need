package com.friendinneed.presence;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PresenceServiceTest {
    @Test
    void savesOnlyTransitionsAndMeasuresReturn() {
        PresenceEventRepository repo = mock(PresenceEventRepository.class);
        UUID id = UUID.randomUUID();
        Instant then = Instant.parse("2026-01-01T10:00:00Z");
        when(repo.findTop1ByProfileIdOrderByCreatedAtDesc(id)).thenReturn(Optional.of(new PresenceEvent(id, PresenceStatus.AWAY, 0, then)));
        PresenceService service = new PresenceService(repo, Clock.fixed(then.plusSeconds(2700), ZoneOffset.UTC));
        var result = service.recordPresence(id, PresenceStatus.PRESENT, 1);
        assertTrue(result.changed());
        assertEquals(45, result.awayDuration().toMinutes());
    }
}
