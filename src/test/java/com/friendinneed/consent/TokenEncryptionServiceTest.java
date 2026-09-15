package com.friendinneed.consent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenEncryptionServiceTest {
    @Test
    void roundTrip() {
        TokenEncryptionService service = new TokenEncryptionService("01234567890123456789012345678901");
        assertEquals("secret", service.decrypt(service.encrypt("secret")));
    }

    @Test
    void wrongKeyFails() {
        String token = new TokenEncryptionService("01234567890123456789012345678901").encrypt("secret");
        assertThrows(IllegalArgumentException.class, () -> new TokenEncryptionService("abcdefghijabcdefghijabcdefghijab").decrypt(token));
    }
}
