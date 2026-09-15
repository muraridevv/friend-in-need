package com.friendinneed;

import com.friendinneed.embedding.EmbeddingProperties;
import com.friendinneed.robot.RobotProperties;
import com.friendinneed.voice.VoiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableConfigurationProperties({EmbeddingProperties.class, VoiceProperties.class, RobotProperties.class})
@SpringBootApplication
public class FriendInNeedApplication {
    public static void main(String[] args) {
        SpringApplication.run(FriendInNeedApplication.class, args);
    }
}
