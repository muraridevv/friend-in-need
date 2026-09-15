package com.friendinneed.memory;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Buffers memory writes while PostgreSQL is unavailable, then retries them in order.
 */
@Service
public class OfflineQueue {
    private static final Logger log = LoggerFactory.getLogger(OfflineQueue.class);
    private final CompanionMemoryRepository memories;
    private final ConcurrentLinkedQueue<MemoryWrite> queued = new ConcurrentLinkedQueue<>();

    public OfflineQueue(CompanionMemoryRepository memories) {
        this.memories = memories;
    }

    public void save(UUID profileId, String content, int importance, float[] embedding) {
        MemoryWrite write = new MemoryWrite(profileId, content, importance, embedding);
        try {
            write(write);
        } catch (DataAccessException | CannotCreateTransactionException error) {
            queued.add(write);
            log.warn("Database unavailable; queued memory write. queueDepth={}", queued.size());
        }
    }

    @Scheduled(fixedDelay = 30_000)
    public void flush() {
        int flushed = 0;
        while (true) {
            MemoryWrite next = queued.peek();
            if (next == null) break;
            try {
                write(next);
                queued.poll();
                flushed++;
            } catch (DataAccessException | CannotCreateTransactionException error) {
                log.warn("Database still unavailable; memory queueDepth={}", queued.size());
                break;
            }
        }
        log.info("Memory queue flush complete: flushed={}, queueDepth={}", flushed, queued.size());
    }

    public int depth() {
        return queued.size();
    }

    private void write(MemoryWrite item) {
        memories.save(new CompanionMemory(item.profileId(), item.content(), item.importance(), item.embedding()));
    }

    private record MemoryWrite(UUID profileId, String content, int importance, float[] embedding) {
    }
}
