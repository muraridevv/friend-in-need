package com.friendinneed;

import com.friendinneed.embedding.EmbeddingService;
import com.friendinneed.memory.CompanionMemory;
import com.friendinneed.memory.CompanionMemoryRepository;
import com.friendinneed.memory.MemoryService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MemoryServiceTest {
    @Test
    void testCosineIdenticalVectors() {
        assertEquals(1.0, MemoryService.cosine(new float[] {1, 2}, new float[] {1, 2}), 0.0001);
    }

    @Test
    void testCosineOrthogonalVectors() {
        assertEquals(0.0, MemoryService.cosine(new float[] {1, 0}, new float[] {0, 1}), 0.0001);
    }

    @Test
    void testLexicalFallback() {
        CompanionMemoryRepository repository = mock(CompanionMemoryRepository.class);
        EmbeddingService embeddings = mock(EmbeddingService.class);
        UUID profileId = UUID.randomUUID();
        when(embeddings.embed(anyString())).thenReturn(new float[0]);
        when(repository.findTop8ByProfileIdOrderByImportanceDescCreatedAtDesc(profileId))
                .thenReturn(List.of(new CompanionMemory(profileId, "I enjoy running in the morning", 1, "[]", "test")));

        String result = new MemoryService(repository, embeddings).relevantTo(profileId, "running plans");
        assertTrue(result.contains("running in the morning"));
    }
}
