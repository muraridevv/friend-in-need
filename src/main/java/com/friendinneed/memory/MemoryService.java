package com.friendinneed.memory;

import com.friendinneed.embedding.EmbeddingService;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MemoryService {
    private final CompanionMemoryRepository memories; private final EmbeddingService embeddings;
    public MemoryService(CompanionMemoryRepository memories, EmbeddingService embeddings) { this.memories=memories; this.embeddings=embeddings; }
    public void remember(UUID profileId, String content, int importance) {
        float[] vector;
        try { vector=embeddings.embed(content); } catch (Exception ignored) { vector=new float[0]; }
        memories.save(new CompanionMemory(profileId, content, importance, EmbeddingService.serialize(vector), embeddings.model()));
    }
    public String relevantTo(UUID profileId, String query) {
        List<CompanionMemory> candidates=memories.findTop8ByProfileIdOrderByImportanceDescCreatedAtDesc(profileId);
        float[] queryVector;
        try { queryVector=embeddings.embed(query); } catch (Exception ignored) { queryVector=new float[0]; }
        Set<String> terms=Arrays.stream(query.toLowerCase(Locale.ROOT).split("\\W+")).filter(word->word.length()>2).collect(Collectors.toSet());
        return candidates.stream().sorted(Comparator.comparingDouble(memory -> score(memory,queryVector,terms)).reversed()).limit(4)
                .map(CompanionMemory::getContent).collect(Collectors.joining("\n- ","- ",""));
    }
    private double score(CompanionMemory memory,float[] query,Set<String> terms) {
        float[] candidate=EmbeddingService.deserialize(memory.getEmbedding());
        if (query.length>0 && candidate.length==query.length) return cosine(query,candidate);
        return Arrays.stream(memory.getContent().toLowerCase(Locale.ROOT).split("\\W+")).filter(terms::contains).count();
    }
    public static double cosine(float[] left,float[] right) { double dot=0,a=0,b=0; for(int i=0;i<left.length;i++){dot+=left[i]*right[i];a+=left[i]*left[i];b+=right[i]*right[i];} return a==0||b==0?0:dot/Math.sqrt(a*b); }
}
