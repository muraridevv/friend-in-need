package com.friendinneed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.friendinneed.memory.CompanionMemory;
import com.friendinneed.memory.CompanionMemoryRepository;
import com.friendinneed.memory.MemoryService;
import com.friendinneed.embedding.EmbeddingService;
import com.friendinneed.profile.CompanionProfile;
import com.friendinneed.profile.CompanionProfileRepository;
import com.friendinneed.security.UserEntity;
import com.friendinneed.security.UserRepository;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
class MemoryServicePgVectorIntegrationTest {
    @Container
    @ServiceConnection // Automatically wires spring.datasource properties in Spring Boot 3.1+
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg17")
                    .asCompatibleSubstituteFor("postgres")
    );

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private UserRepository users;
    @Autowired private CompanionProfileRepository profiles;
    @Autowired private CompanionMemoryRepository memories;
    @Autowired private MemoryService memoryService;
    @MockBean
    private EmbeddingService embeddingService;

    @Test
    void ordersMemoriesByPgVectorCosineDistance() {
        UserEntity user = users.save(new UserEntity("vector-user", "hash"));
        CompanionProfile profile = profiles.save(new CompanionProfile(user.getId(), "Sam", "warm", "music", "UTC", "New York"));
        memories.save(new CompanionMemory(profile.getId(), "near", 1, vector(1, 0)));
        memories.save(new CompanionMemory(profile.getId(), "far", 1, vector(0, 1)));

        when(embeddingService.embed("query")).thenReturn(vector(1, 0));
        String result = memoryService.relevantTo(profile.getId(), "query");
        assertEquals("- near", result);
    }

    private float[] vector(float first, float second) {
        float[] vector = new float[1536]; vector[0] = first; vector[1] = second; return vector;
    }
}
