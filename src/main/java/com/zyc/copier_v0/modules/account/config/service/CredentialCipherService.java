package com.zyc.copier_v0.modules.account.config.service;

import com.zyc.copier_v0.modules.account.config.config.CredentialCryptoProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class CredentialCipherService {

    private static final String AES = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH = 12;

    private final CredentialCryptoProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public CredentialCipherService(CredentialCryptoProperties properties) {
        this.properties = properties;
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt MT5 credential", ex);
        }
    }

    private SecretKeySpec keySpec() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] key = digest.digest(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(key, AES);
    }
}
