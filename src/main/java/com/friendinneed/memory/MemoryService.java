package com.friendinneed.memory;

import com.friendinneed.embedding.EmbeddingService;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MemoryService {
    private final CompanionMemoryRepository memories;
    private final EmbeddingService embeddings;

    public MemoryService(CompanionMemoryRepository memories, EmbeddingService embeddings) {
        this.memories = memories; this.embeddings = embeddings;
    }

    public void remember(UUID profileId, String content, int importance) {
        float[] vector = embeddingFor(content);
        memories.save(new CompanionMemory(profileId, content, importance, vector.length == 0 ? null : vector));
    }

    public String relevantTo(UUID profileId, String query) {
        float[] queryVector = embeddingFor(query);
        List<CompanionMemory> relevant = queryVector.length == 0
                ? lexicalFallback(profileId, query)
                : memories.findSimilar(profileId, toVectorLiteral(queryVector), 4);
        return relevant.stream().map(CompanionMemory::getContent).collect(Collectors.joining("\n- ", "- ", ""));
    }

    private float[] embeddingFor(String text) {
        try { return embeddings.embed(text); } catch (Exception ignored) { return new float[0]; }
    }

    private List<CompanionMemory> lexicalFallback(UUID profileId, String query) {
        Set<String> terms = Arrays.stream(query.toLowerCase(Locale.ROOT).split("\\W+"))
                .filter(word -> word.length() > 2).collect(Collectors.toSet());
        return memories.findTop8ByProfileIdOrderByImportanceDescCreatedAtDesc(profileId).stream()
                .filter(memory -> terms.stream().anyMatch(term -> memory.getContent().toLowerCase(Locale.ROOT).contains(term)))
                .limit(4).toList();
    }

    private String toVectorLiteral(float[] vector) {
        return Arrays.toString(vector);
    }
}
