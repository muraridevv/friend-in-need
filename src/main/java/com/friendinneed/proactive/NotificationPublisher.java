package com.friendinneed.proactive;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationPublisher {
    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter connect(UUID profileId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.put(profileId, emitter);
        emitter.onCompletion(() -> emitters.remove(profileId, emitter));
        emitter.onTimeout(() -> emitters.remove(profileId, emitter));
        return emitter;
    }

    public boolean publish(ProactiveMessage message) {
        SseEmitter emitter = emitters.get(message.getProfileId());
        if (emitter == null) return false;
        try {
            emitter.send(SseEmitter.event().name("proactive").data(message.getContent()));
            return true;
        } catch (IOException exception) {
            emitters.remove(message.getProfileId(), emitter);
            emitter.completeWithError(exception);
            return false;
        }
    }
}
