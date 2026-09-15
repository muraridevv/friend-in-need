package com.friendinneed.routine;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoutineRepository extends JpaRepository<Routine, UUID> {
    List<Routine> findByProfileId(UUID profileId);

    Optional<Routine> findByProfileIdAndTriggerPhraseIgnoreCase(UUID profileId, String triggerPhrase);
}
