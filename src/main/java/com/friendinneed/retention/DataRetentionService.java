package com.friendinneed.retention;

import com.friendinneed.conversation.ConversationMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class DataRetentionService {
    private static final Logger log = LoggerFactory.getLogger(DataRetentionService.class);
    private final ConversationMessageRepository messages;
    private final int retentionDays;

    public DataRetentionService(ConversationMessageRepository messages, @Value("${retention.days:90}") int retentionDays) {
        this.messages = messages;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteExpiredMessages() {
        int deleted = messages.deleteByCreatedAtBefore(Instant.now().minus(retentionDays, ChronoUnit.DAYS));
        log.info("Deleted {} conversation messages older than {} days", deleted, retentionDays);
    }
}
