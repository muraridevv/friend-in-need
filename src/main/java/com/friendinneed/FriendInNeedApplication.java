package com.friendinneed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.friendinneed.embedding.EmbeddingProperties;
import com.friendinneed.voice.VoiceProperties;

@EnableScheduling
@EnableConfigurationProperties({EmbeddingProperties.class, VoiceProperties.class})
@SpringBootApplication
public class FriendInNeedApplication {
  public static void main(String[] args) { SpringApplication.run(FriendInNeedApplication.class, args); }
}
