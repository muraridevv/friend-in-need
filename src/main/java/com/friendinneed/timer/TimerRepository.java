package com.friendinneed.timer;

import java.time.Instant;
import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TimerRepository extends JpaRepository<Timer, UUID> {
    List<Timer> findByProfileIdOrderByTriggerAt(UUID id);

    List<Timer> findByFiredFalseAndTriggerAtLessThanEqual(Instant now);
}
