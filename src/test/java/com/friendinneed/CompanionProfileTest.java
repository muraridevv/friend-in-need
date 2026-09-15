package com.friendinneed;

import com.friendinneed.profile.CompanionProfile;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CompanionProfileTest {
    private final String template = "10".repeat(32);

    @Test
    void testFaceMatchAboveThreshold() {
        CompanionProfile profile = profile();
        profile.enrollFace(template);
        assertTrue(profile.faceMatch(template).recognized());
    }

    @Test
    void testFaceMatchBelowThreshold() {
        CompanionProfile profile = profile();
        profile.enrollFace(template);
        assertFalse(profile.faceMatch("01".repeat(32)).recognized());
    }

    @Test
    void testFaceMatchNullFingerprint() {
        CompanionProfile.FaceMatch match = profile().faceMatch(template);
        assertFalse(match.recognized());
        assertEquals(0, match.confidence());
    }

    @Test
    void testFaceMatchMultipleTemplates() {
        CompanionProfile profile = profile();
        String different = "01".repeat(32);
        profile.enrollFace(different + "|" + template);
        assertTrue(profile.faceMatch(different + "|" + template).recognized());
        assertEquals(100, profile.faceMatch(different + "|" + template).confidence());
    }

    private CompanionProfile profile() {
        return new CompanionProfile("Sam", "warm", "music", "UTC", "New York");
    }
}
