package com.friendinneed.memory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface CompanionMemoryRepository extends JpaRepository<CompanionMemory, UUID> {
    List<CompanionMemory> findTop8ByProfileIdOrderByImportanceDescCreatedAtDesc(UUID profileId);
}
