package com.friendinneed.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class UserEntity {
    @Id private UUID id;
    @Column(nullable = false, unique = true) private String username;
    @Column(nullable = false) private String passwordHash;
    @Column(nullable = false) private Instant createdAt;

    protected UserEntity() { }

    public UserEntity(String username, String passwordHash) {
        this.id = UUID.randomUUID();
        this.username = username;
        this.passwordHash = passwordHash;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
}
