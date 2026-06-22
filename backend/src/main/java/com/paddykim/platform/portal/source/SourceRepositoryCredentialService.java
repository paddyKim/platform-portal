package com.paddykim.platform.portal.source;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SourceRepositoryCredentialService {

    private static final String STORAGE_PREFIX = "v1";
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;
    private static final OAEPParameterSpec NETWORK_OAEP_SPEC = new OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT
    );

    private final SecureRandom secureRandom = new SecureRandom();
    private final String storageSecret;
    private KeyPair networkKeyPair;

    public SourceRepositoryCredentialService(
            @Value("${portal.credentials.storage-secret:local-development-credential-secret-change-me}")
            String storageSecret
    ) {
        this.storageSecret = storageSecret;
    }

    @PostConstruct
    void initializeNetworkKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            networkKeyPair = generator.generateKeyPair();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to initialize source repository credential encryption", exception);
        }
    }

    public String networkPublicKey() {
        return Base64.getEncoder().encodeToString(networkKeyPair.getPublic().getEncoded());
    }

    public String encryptForNetwork(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, networkKeyPair.getPublic(), NETWORK_OAEP_SPEC);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (GeneralSecurityException exception) {
            throw new SourceRepositoryValidationException("Unable to encrypt source repository credential");
        }
    }

    public String decryptFromNetwork(String encryptedText) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            cipher.init(Cipher.DECRYPT_MODE, networkKeyPair.getPrivate(), NETWORK_OAEP_SPEC);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            throw new SourceRepositoryValidationException("Invalid encrypted source repository credential");
        }
    }

    public String encryptForStorage(String plainText) {
        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, storageKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            return String.join(":",
                    STORAGE_PREFIX,
                    Base64.getEncoder().encodeToString(iv),
                    Base64.getEncoder().encodeToString(encrypted)
            );
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to encrypt source repository credential for storage", exception);
        }
    }

    public String decryptFromStorage(String storedText) {
        if (storedText == null || storedText.isBlank()) {
            return "";
        }

        String[] parts = storedText.split(":", 3);
        if (parts.length != 3 || !STORAGE_PREFIX.equals(parts[0])) {
            throw new SourceRepositoryValidationException("Invalid stored source repository credential");
        }

        try {
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] encrypted = Base64.getDecoder().decode(parts[2]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, storageKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            throw new SourceRepositoryValidationException("Unable to decrypt stored source repository credential");
        }
    }

    private SecretKeySpec storageKey() {
        try {
            byte[] key = MessageDigest.getInstance("SHA-256")
                    .digest(storageSecret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(key, "AES");
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to initialize source repository credential storage key", exception);
        }
    }
}
