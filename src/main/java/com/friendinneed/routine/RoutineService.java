package com.friendinneed.routine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.friendinneed.smarthome.SmartHomeAdapter;

import java.util.*;

import org.springframework.stereotype.Service;

@Service
public class RoutineService {
    private final RoutineRepository routines;
    private final SmartHomeAdapter home;
    private final ObjectMapper json;

    public RoutineService(RoutineRepository r, SmartHomeAdapter h, ObjectMapper j) {
        routines = r;
        home = h;
        json = j;
    }

    public List<Routine> list(UUID p) {
        return routines.findByProfileId(p);
    }

    public Routine create(UUID p, String phrase, List<Map<String, Object>> steps) {
        try {
            return routines.save(new Routine(p, phrase, json.writeValueAsString(steps)));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid routine steps", e);
        }
    }

    public void delete(UUID p, UUID id) {
        Routine r = routines.findById(id).filter(x -> x.getProfileId().equals(p)).orElseThrow();
        routines.delete(r);
    }

    public boolean executeForPhrase(UUID p, String phrase) {
        Routine r = routines.findByProfileIdAndTriggerPhraseIgnoreCase(p, phrase).orElse(null);
        if (r == null && phrase.equalsIgnoreCase("Goodnight Nova")) {
            execute(List.of(Map.of("entityId", "light.living_room", "command", "turn_off"), Map.of("entityId", "lock.front_door", "command", "lock")));
            return true;
        }
        if (r == null) return false;
        try {
            execute(json.readValue(r.getSteps(), new TypeReference<List<Map<String, Object>>>() {
            }));
            return true;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to execute routine", e);
        }
    }

    private void execute(List<Map<String, Object>> steps) {
        for (Map<String, Object> s : steps) {
            String entity = (String) s.get("entityId"), command = (String) s.get("command");
            if (entity == null || command == null || !entity.matches("(light|lock|climate|media_player)\\.[a-zA-Z0-9_]+") || !Set.of("turn_on", "turn_off", "lock", "unlock", "set_temperature").contains(command))
                throw new IllegalArgumentException("Routine command is not allowed");
            if (home.listDevices().stream().noneMatch(d -> d.entityId().equals(entity)))
                throw new IllegalArgumentException("Unknown routine device");
            home.sendCommand(entity, command, (Map<String, Object>) s.getOrDefault("params", Map.of()));
        }
    }
}
