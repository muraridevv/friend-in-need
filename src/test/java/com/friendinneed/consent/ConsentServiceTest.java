package com.friendinneed.consent;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ConsentServiceTest {
    @Test
    void grantRevokeAndCheck() {
        ConsentRepository repo = mock(ConsentRepository.class);
        UUID id = UUID.randomUUID();
        when(repo.findByProfileIdAndIntegrationType(id, IntegrationType.NEWS)).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        ConsentService service = new ConsentService(repo);
        assertTrue(service.grant(id, IntegrationType.NEWS).isEnabled());
        assertFalse(service.revoke(id, IntegrationType.NEWS).isEnabled());
    }
}
