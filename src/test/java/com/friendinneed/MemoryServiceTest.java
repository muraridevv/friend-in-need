package com.friendinneed;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.friendinneed.embedding.EmbeddingService;
import com.friendinneed.memory.CompanionMemory;
import com.friendinneed.memory.CompanionMemoryRepository;
import com.friendinneed.memory.MemoryService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MemoryServiceTest {
    @Test void usesLexicalFallbackWhenEmbeddingIsUnavailable() {
        CompanionMemoryRepository repository = mock(CompanionMemoryRepository.class); EmbeddingService embeddings = mock(EmbeddingService.class); UUID profileId = UUID.randomUUID();
        when(embeddings.embed(anyString())).thenReturn(new float[0]);
        when(repository.findTop8ByProfileIdOrderByImportanceDescCreatedAtDesc(profileId)).thenReturn(List.of(new CompanionMemory(profileId, "I enjoy running in the morning", 1, null)));
        assertTrue(new MemoryService(repository, embeddings).relevantTo(profileId, "running plans").contains("running in the morning"));
    }
    @Test void delegatesVectorSimilarityToDatabase() {
        CompanionMemoryRepository repository = mock(CompanionMemoryRepository.class); EmbeddingService embeddings = mock(EmbeddingService.class); UUID profileId = UUID.randomUUID();
        when(embeddings.embed("travel")).thenReturn(new float[] {0.1f, 0.2f}); when(repository.findSimilar(eq(profileId), anyString(), eq(4))).thenReturn(List.of());
        new MemoryService(repository, embeddings).relevantTo(profileId, "travel");
        org.mockito.Mockito.verify(repository).findSimilar(eq(profileId), eq("[0.1, 0.2]"), eq(4));
    }
}
