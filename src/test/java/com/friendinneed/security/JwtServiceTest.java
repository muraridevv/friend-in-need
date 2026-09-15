package com.friendinneed.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private final JwtService jwtService = new JwtService("a-test-secret-that-is-at-least-thirty-two-bytes");

    @Test
    void generatesValidTokenWithUsernameSubject() {
        String token = jwtService.generateToken("alex");
        assertTrue(jwtService.validateToken(token));
        assertEquals("alex", jwtService.extractUsername(token));
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        String token = jwtService.generateToken("alex");
        assertFalse(new JwtService("another-test-secret-that-is-at-least-32").validateToken(token));
    }
}
