package com.friendinneed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class FriendInNeedApplication {
  public static void main(String[] args) { SpringApplication.run(FriendInNeedApplication.class, args); }
}
