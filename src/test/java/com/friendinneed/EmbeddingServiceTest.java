package com.friendinneed;

import com.friendinneed.embedding.EmbeddingService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class EmbeddingServiceTest {
    @Test
    void testSerializeDeserializeRoundtrip() {
        float[] vector = {0.25f, -0.5f, 1.0f};
        assertArrayEquals(vector, EmbeddingService.deserialize(EmbeddingService.serialize(vector)));
    }

    @Test
    void testDeserializeNull() {
        assertArrayEquals(new float[0], EmbeddingService.deserialize(null));
    }

    @Test
    void testDeserializeEmpty() {
        assertArrayEquals(new float[0], EmbeddingService.deserialize(""));
    }
}
