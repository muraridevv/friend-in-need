package com.friendinneed.routing;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

@Service
public class ModelRouter {
    private static final long MAX_CLOUD_LATENCY_MILLIS = 5_000;
    private final HealthPinger openRouter;
    private final HealthPinger voice;
    private final HealthPinger embedding;
    private final String openRouterUrl;
    private final String voiceUrl;
    private final String embeddingUrl;
    private final boolean preferLocal;
    private final boolean allowSimulateOffline;

    public ModelRouter(@Qualifier("openRouterHealthPinger") HealthPinger openRouter, @Qualifier("voiceHealthPinger") HealthPinger voice, @Qualifier("embeddingHealthPinger") HealthPinger embedding,
                       @Value("${spring.ai.openai.base-url:}") String openRouterUrl,
                       @Value("${voice.base-url:}") String voiceUrl,
                       @Value("${embedding.base-url:}") String embeddingUrl,
                       @Value("${routing.prefer-local:false}") boolean preferLocal, @Value("${routing.allow-simulate-offline:false}") boolean allowSimulateOffline) {
        this.openRouter = openRouter;
        this.voice = voice;
        this.embedding = embedding;
        this.openRouterUrl = openRouterUrl;
        this.voiceUrl = voiceUrl;
        this.embeddingUrl = embeddingUrl;
        this.preferLocal = preferLocal;
        this.allowSimulateOffline = allowSimulateOffline;
    }

    @Scheduled(fixedDelay = 30_000)
    public void refreshHealth() {
        openRouter.ping(openRouterUrl);
        voice.ping(voiceUrl);
        embedding.ping(embeddingUrl);
    }

    public ModelChoice routeChat() {
        return choose(openRouter);
    }

    public ModelChoice routeStt() {
        return choose(voice);
    }

    public ModelChoice routeTts() {
        return choose(voice);
    }

    public Map<String, String> status(boolean databaseQueuing) {
        return Map.of("chat", statusFor(routeChat()), "stt", statusFor(routeStt()), "tts", statusFor(routeTts()),
                "database", databaseQueuing ? "queuing" : "connected");
    }

    private String statusFor(ModelChoice choice) {
        return choice == ModelChoice.CLOUD ? "cloud" : "local";
    }

    private ModelChoice choose(HealthPinger pinger) {
        if (preferLocal || simulatedOffline()) return ModelChoice.LOCAL;
        return pinger.healthy() && pinger.averageLatencyMillis() < MAX_CLOUD_LATENCY_MILLIS ? ModelChoice.CLOUD : ModelChoice.LOCAL;
    }

    private boolean simulatedOffline() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return false;
        HttpServletRequest request = attributes.getRequest();
        return allowSimulateOffline && "true".equalsIgnoreCase(request.getHeader("X-Simulate-Offline"));
    }
}
