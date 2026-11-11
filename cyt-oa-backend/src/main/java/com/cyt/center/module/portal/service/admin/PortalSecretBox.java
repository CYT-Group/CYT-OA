package com.cyt.center.module.portal.service.admin;

import com.cyt.center.core.exception.BizException;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** 与旧 Portal v1:base64(nonce|ciphertext|tag) 格式兼容，主密钥只从环境读取。 */
@Component
public class PortalSecretBox {
    private final java.util.function.Supplier<String> masterSource;

    public PortalSecretBox() {
        this(() -> System.getenv().getOrDefault("AI_KEY_ENCRYPTION_KEY", ""));
    }

    PortalSecretBox(java.util.function.Supplier<String> source) {
        this.masterSource = source;
    }

    private byte[] master() {
        try {
            byte[] value = Base64.getDecoder().decode(masterSource.get());
            if (value.length == 32) return value;
        } catch (IllegalArgumentException ignored) {
        }
        throw new BizException("请配置 AI_KEY_ENCRYPTION_KEY（Base64 编码的32字节主密钥）");
    }

    public String encrypt(String plain) {
        byte[] key = master();
        try {
            byte[] nonce = new byte[12];
            new SecureRandom().nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] blob = Arrays.copyOf(nonce, nonce.length + encrypted.length);
            System.arraycopy(encrypted, 0, blob, nonce.length, encrypted.length);
            return "v1:" + Base64.getEncoder().encodeToString(blob);
        } catch (Exception e) {
            throw new BizException("密钥加密失败");
        }
    }

    public String decrypt(String token) {
        byte[] key = master();
        try {
            if (!token.startsWith("v1:")) throw new IllegalArgumentException();
            byte[] blob = Base64.getDecoder().decode(token.substring(3));
            if (blob.length < 28) throw new IllegalArgumentException();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(128, Arrays.copyOf(blob, 12)));
            return new String(
                    cipher.doFinal(Arrays.copyOfRange(blob, 12, blob.length)),
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BizException("已保存密钥无法解密，请核对加密主密钥");
        }
    }
}
