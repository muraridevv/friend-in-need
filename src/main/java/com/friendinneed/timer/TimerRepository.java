package com.friendinneed.timer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TimerRepository extends JpaRepository<Timer, UUID> {
    List<Timer> findByProfileIdOrderByTriggerAt(UUID id);

    List<Timer> findByFiredFalseAndTriggerAtLessThanEqual(Instant now);
}
