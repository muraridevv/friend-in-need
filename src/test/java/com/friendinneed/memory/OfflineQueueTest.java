package com.friendinneed.memory;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class OfflineQueueTest {
    @Test
    void queuesFailedWriteAndFlushesItAfterDatabaseRecovers() {
        CompanionMemoryRepository repository = mock(CompanionMemoryRepository.class);
        OfflineQueue queue = new OfflineQueue(repository);
        doThrow(new DataAccessResourceFailureException("down")).doNothing().when(repository).save(org.mockito.ArgumentMatchers.any());
        queue.save(UUID.randomUUID(), "likes tea", 3, new float[]{1f});
        assertEquals(1, queue.depth());
        queue.flush();
        assertEquals(0, queue.depth());
        verify(repository, org.mockito.Mockito.times(2)).save(org.mockito.ArgumentMatchers.any());
    }
}
