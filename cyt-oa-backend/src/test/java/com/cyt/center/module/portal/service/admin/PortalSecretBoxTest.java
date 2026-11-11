package com.cyt.center.module.portal.service.admin;

import static org.junit.jupiter.api.Assertions.*;

import com.cyt.center.core.exception.BizException;

import org.junit.jupiter.api.Test;

import java.util.Base64;

class PortalSecretBoxTest {
    private final PortalSecretBox box =
            new PortalSecretBox(() -> Base64.getEncoder().encodeToString(new byte[32]));

    @Test
    void randomNonceAndAuthenticatedRoundtrip() {
        String one = box.encrypt("private-test-key"), two = box.encrypt("private-test-key");
        assertTrue(one.startsWith("v1:"));
        assertNotEquals(one, two);
        assertFalse(one.contains("private-test-key"));
        assertEquals("private-test-key", box.decrypt(one));
    }

    @Test
    void tamperingAndMissingMasterFailClosed() {
        String token = box.encrypt("secret");
        byte[] blob = Base64.getDecoder().decode(token.substring(3));
        blob[12] ^= 1;
        assertThrows(
                BizException.class,
                () -> box.decrypt("v1:" + Base64.getEncoder().encodeToString(blob)));
        assertThrows(BizException.class, () -> new PortalSecretBox(() -> "").encrypt("secret"));
    }
}
