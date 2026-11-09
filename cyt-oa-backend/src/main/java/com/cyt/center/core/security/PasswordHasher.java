package com.cyt.center.core.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * 口令散列：PBKDF2-HMAC-SHA256，存储格式 pbkdf2:sha256:迭代数$盐$散列。
 * 只服务本地账号密码登录；线上关闭口令登录后本类不参与判定。
 */
public final class PasswordHasher {

    private static final String PREFIX = "pbkdf2:sha256:";
    private static final int ITERATIONS = 210_000;
    private static final int KEY_LENGTH = 256;

    private PasswordHasher() {
    }

    public static String hash(String rawPassword) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return PREFIX + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(derive(rawPassword, salt, ITERATIONS));
    }

    /** 定长比较，避免用响应时间推断散列内容。 */
    public static boolean matches(String rawPassword, String stored) {
        if (rawPassword == null || stored == null || !stored.startsWith(PREFIX)) {
            return false;
        }
        String[] parts = stored.substring(PREFIX.length()).split("\\$");
        if (parts.length != 3) {
            return false;
        }
        try {
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);
            return MessageDigest.isEqual(expected, derive(rawPassword, salt, Integer.parseInt(parts[0])));
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    private static byte[] derive(String rawPassword, byte[] salt, int iterations) {
        try {
            KeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, iterations, KEY_LENGTH);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (java.security.GeneralSecurityException error) {
            throw new IllegalStateException("JDK 缺少 PBKDF2WithHmacSHA256 实现", error);
        }
    }

}
