package com.friendinneed.memory;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.*;

@Service
public class MemoryService {
    private final CompanionMemoryRepository memories;
    public MemoryService(CompanionMemoryRepository memories) { this.memories = memories; }
    public void remember(UUID profileId, String content, int importance) { memories.save(new CompanionMemory(profileId, content, importance)); }
    /** Lightweight retrieval now; replace this port with pgvector semantic search as memory volume grows. */
    public String relevantTo(UUID profileId, String query) {
        Set<String> terms = Arrays.stream(query.toLowerCase(Locale.ROOT).split("\\W+"))
                .filter(word -> word.length() > 2).collect(Collectors.toSet());
        return memories.findTop8ByProfileIdOrderByImportanceDescCreatedAtDesc(profileId).stream()
                .sorted(Comparator.comparingInt(m -> score(m.getContent(), terms)).reversed())
                .limit(4).map(CompanionMemory::getContent).collect(Collectors.joining("\n- ", "- ", ""));
    }
    private int score(String content, Set<String> terms) { return (int) Arrays.stream(content.toLowerCase(Locale.ROOT).split("\\W+")).filter(terms::contains).count(); }
}
