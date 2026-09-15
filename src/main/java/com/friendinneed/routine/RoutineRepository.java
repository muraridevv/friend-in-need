package com.friendinneed.routine;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutineRepository extends JpaRepository<Routine, UUID> {
    List<Routine> findByProfileId(UUID profileId);

    Optional<Routine> findByProfileIdAndTriggerPhraseIgnoreCase(UUID profileId, String triggerPhrase);
}
