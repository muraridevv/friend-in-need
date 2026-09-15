package com.friendinneed.consent;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenEncryptionService {
    private final SecretKeySpec key;

    public TokenEncryptionService(@Value("${token-encryption-key:change-me-change-me-change-me-32}") String secret) {
        key = new SecretKeySpec(Arrays.copyOf(secret.getBytes(StandardCharsets.UTF_8), 32), "AES");
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] value = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, value, 0, iv.length);
            System.arraycopy(encrypted, 0, value, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to encrypt token", e);
        }
    }

    public String decrypt(String ciphertext) {
        try {
            byte[] value = Base64.getDecoder().decode(ciphertext);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, value, 0, 12));
            return new String(cipher.doFinal(value, 12, value.length - 12), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to decrypt token", e);
        }
    }
}
