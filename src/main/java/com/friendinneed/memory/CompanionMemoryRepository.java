package com.friendinneed.memory;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanionMemoryRepository extends JpaRepository<CompanionMemory, UUID> {
    List<CompanionMemory> findTop8ByProfileIdOrderByImportanceDescCreatedAtDesc(UUID profileId);

    @Query(value = "SELECT * FROM companion_memory WHERE profile_id = :profileId "
            + "ORDER BY embedding <=> cast(:queryVec as vector) LIMIT :limit", nativeQuery = true)
    List<CompanionMemory> findSimilar(@Param("profileId") UUID profileId,
            @Param("queryVec") String queryVec, @Param("limit") int limit);
}
