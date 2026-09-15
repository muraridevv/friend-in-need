package com.friendinneed.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class OfflineQueueTest {
    @Test void queuesFailedWriteAndFlushesItAfterDatabaseRecovers() {
        CompanionMemoryRepository repository = mock(CompanionMemoryRepository.class);
        OfflineQueue queue = new OfflineQueue(repository);
        doThrow(new DataAccessResourceFailureException("down")).doNothing().when(repository).save(org.mockito.ArgumentMatchers.any());
        queue.save(UUID.randomUUID(), "likes tea", 3, new float[] {1f});
        assertEquals(1, queue.depth());
        queue.flush();
        assertEquals(0, queue.depth());
        verify(repository, org.mockito.Mockito.times(2)).save(org.mockito.ArgumentMatchers.any());
    }
}
