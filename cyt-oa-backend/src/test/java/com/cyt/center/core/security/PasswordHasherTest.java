package com.cyt.center.core.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    /** 与 db/postgresql/migration/V20261112_002__initial_seed.sql 里 dev 账号的散列保持一致；改种子文件必须同步改这里。 */
    private static final String SEEDED_DEV_HASH =
            "pbkdf2:sha256:210000$m3g503RNYWhLM6ctXjG2dw==$UHKQeq5ibcoaOaW1xVYAJjdhzDh1IXfx9VAeG7+WjHE=";

    @Test
    void seededDevAccountAcceptsAdmin123() {
        assertTrue(PasswordHasher.matches("admin123", SEEDED_DEV_HASH));
        assertFalse(PasswordHasher.matches("admin1234", SEEDED_DEV_HASH));
    }

    @Test
    void hashRoundTripsWithRandomSalt() {
        String first = PasswordHasher.hash("Hunter2!cyt");
        String second = PasswordHasher.hash("Hunter2!cyt");
        assertNotEquals(first, second);
        assertTrue(PasswordHasher.matches("Hunter2!cyt", first));
        assertTrue(PasswordHasher.matches("Hunter2!cyt", second));
    }

    @Test
    void malformedStorageIsRejectedInsteadOfThrowing() {
        assertFalse(PasswordHasher.matches("admin123", null));
        assertFalse(PasswordHasher.matches(null, SEEDED_DEV_HASH));
        // 迁移自 ruoyi 的 BCrypt 串、段数不对的串、Base64 非法的串都不能放行
        assertFalse(PasswordHasher.matches("admin123", "$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2"));
        assertFalse(PasswordHasher.matches("admin123", "pbkdf2:sha256:210000$onlytwo$parts$here"));
        assertFalse(PasswordHasher.matches("admin123", "pbkdf2:sha256:210000$###$###"));
    }
}
