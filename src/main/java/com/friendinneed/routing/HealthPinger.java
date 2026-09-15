package com.friendinneed.routing;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;

/** Performs a lightweight provider reachability check and retains its rolling latency. */
public class HealthPinger {
    private final RestClient client;
    private volatile Instant lastSuccess;
    private volatile Instant lastFailure;
    private final AtomicLong averageLatencyMillis = new AtomicLong(Long.MAX_VALUE);

    public HealthPinger() {
        this(RestClient.builder().build());
    }

    HealthPinger(RestClient client) {
        this.client = client;
    }

    public boolean ping(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            lastFailure = Instant.now();
            return false;
        }
        long started = System.nanoTime();
        try {
            client.method(HttpMethod.HEAD).uri(baseUrl).retrieve().toBodilessEntity();
        } catch (Exception headFailure) {
            try { client.method(HttpMethod.GET).uri(baseUrl).retrieve().toBodilessEntity(); }
            catch (Exception getFailure) { lastFailure = Instant.now(); return false; }
        }
        recordSuccess(Duration.ofNanos(System.nanoTime() - started).toMillis());
        return true;
    }

    public void recordSuccess(long latencyMillis) {
        lastSuccess = Instant.now();
        averageLatencyMillis.updateAndGet(current -> current == Long.MAX_VALUE ? latencyMillis : (current + latencyMillis) / 2);
    }

    public void recordFailure() { lastFailure = Instant.now(); }
    public Instant lastSuccess() { return lastSuccess; }
    public Instant lastFailure() { return lastFailure; }
    public long averageLatencyMillis() { return averageLatencyMillis.get(); }
    public boolean healthy() { return lastSuccess != null && (lastFailure == null || lastSuccess.isAfter(lastFailure)); }
}
