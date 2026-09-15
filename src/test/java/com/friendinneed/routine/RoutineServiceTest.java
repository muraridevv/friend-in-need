package com.friendinneed.routine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.friendinneed.smarthome.SmartHomeAdapter;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

class RoutineServiceTest {
    @Test
    void executesEveryStepInSequence() {
        RoutineRepository repo = mock(RoutineRepository.class);
        SmartHomeAdapter home = mock(SmartHomeAdapter.class);
        RoutineService service = new RoutineService(repo, home, new ObjectMapper());
        Routine r = new Routine(UUID.randomUUID(), "Bedtime", "[{\"entityId\":\"light.living_room\",\"command\":\"turn_off\"},{\"entityId\":\"lock.front_door\",\"command\":\"lock\"}]");
        when(repo.findByProfileIdAndTriggerPhraseIgnoreCase(r.getProfileId(), "Bedtime")).thenReturn(Optional.of(r));
        service.executeForPhrase(r.getProfileId(), "Bedtime");
        verify(home).sendCommand("light.living_room", "turn_off", Map.of());
        verify(home).sendCommand("lock.front_door", "lock", Map.of());
    }
}
