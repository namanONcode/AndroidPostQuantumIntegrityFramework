package com.anchorpq.server.crypto;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for AES-GCM encryption/decryption operations. */
@QuarkusTest
class AesGcmServiceTest {

    @Inject AesGcmService aesGcmService;

    @Inject MLKemService mlKemService;

    @Test
    @DisplayName("Should derive key from shared secret")
    void testKeyDerivation() {
        byte[] sharedSecret = new byte[32];
        new java.security.SecureRandom().nextBytes(sharedSecret);

        SecretKey key = aesGcmService.deriveKey(sharedSecret);

        assertNotNull(key);
        assertEquals("AES", key.getAlgorithm());
        assertEquals(32, key.getEncoded().length); // 256 bits
    }

    @Test
    @DisplayName("Should derive same key from same secret")
    void testKeyDerivationDeterminism() {
        byte[] sharedSecret = new byte[32];
        new java.security.SecureRandom().nextBytes(sharedSecret);

        SecretKey key1 = aesGcmService.deriveKey(sharedSecret);
        SecretKey key2 = aesGcmService.deriveKey(sharedSecret);

        assertArrayEquals(
                key1.getEncoded(), key2.getEncoded(), "Same shared secret should derive same key");
    }

    @Test
    @DisplayName("Should derive different keys from different secrets")
    void testKeyDerivationUniqueness() {
        byte[] secret1 = new byte[32];
        byte[] secret2 = new byte[32];
        new java.security.SecureRandom().nextBytes(secret1);
        new java.security.SecureRandom().nextBytes(secret2);

        SecretKey key1 = aesGcmService.deriveKey(secret1);
        SecretKey key2 = aesGcmService.deriveKey(secret2);

        assertFalse(
                java.util.Arrays.equals(key1.getEncoded(), key2.getEncoded()),
                "Different secrets should derive different keys");
    }

    @Test
    @DisplayName("Should encrypt and decrypt string successfully")
    void testEncryptDecryptString() {
        byte[] sharedSecret = new byte[32];
        new java.security.SecureRandom().nextBytes(sharedSecret);
        SecretKey key = aesGcmService.deriveKey(sharedSecret);

        String plaintext = "Hello, AnchorPQ! This is a test message.";

        String encrypted = aesGcmService.encrypt(key, plaintext);
        String decrypted = aesGcmService.decryptToString(key, encrypted);

        assertEquals(plaintext, decrypted);
    }

    @Test
    @DisplayName("Should encrypt and decrypt bytes successfully")
    void testEncryptDecryptBytes() {
        byte[] sharedSecret = new byte[32];
        new java.security.SecureRandom().nextBytes(sharedSecret);
        SecretKey key = aesGcmService.deriveKey(sharedSecret);

        byte[] plaintext = "Test binary data".getBytes(StandardCharsets.UTF_8);

        String encrypted = aesGcmService.encrypt(key, plaintext);
        byte[] decrypted = aesGcmService.decrypt(key, encrypted);

        assertArrayEquals(plaintext, decrypted);
    }

    @Test
    @DisplayName("Should produce different ciphertext for same plaintext (IV randomness)")
    void testEncryptionNonDeterminism() {
        byte[] sharedSecret = new byte[32];
        new java.security.SecureRandom().nextBytes(sharedSecret);
        SecretKey key = aesGcmService.deriveKey(sharedSecret);

        String plaintext = "Same message";

        String encrypted1 = aesGcmService.encrypt(key, plaintext);
        String encrypted2 = aesGcmService.encrypt(key, plaintext);

        assertNotEquals(
                encrypted1,
                encrypted2,
                "Same plaintext should produce different ciphertext due to random IV");
    }

    @Test
    @DisplayName("Should fail decryption with wrong key")
    void testDecryptionWithWrongKey() {
        byte[] secret1 = new byte[32];
        byte[] secret2 = new byte[32];
        new java.security.SecureRandom().nextBytes(secret1);
        new java.security.SecureRandom().nextBytes(secret2);

        SecretKey key1 = aesGcmService.deriveKey(secret1);
        SecretKey key2 = aesGcmService.deriveKey(secret2);

        String plaintext = "Secret message";
        String encrypted = aesGcmService.encrypt(key1, plaintext);

        CryptoException exception =
                assertThrows(
                        CryptoException.class,
                        () -> {
                            aesGcmService.decryptToString(key2, encrypted);
                        });

        assertEquals(CryptoException.ErrorCode.AUTHENTICATION_FAILED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should fail decryption with tampered ciphertext")
    void testDecryptionWithTamperedCiphertext() {
        byte[] sharedSecret = new byte[32];
        new java.security.SecureRandom().nextBytes(sharedSecret);
        SecretKey key = aesGcmService.deriveKey(sharedSecret);

        String plaintext = "Secret message";
        String encrypted = aesGcmService.encrypt(key, plaintext);

        // Tamper with the ciphertext
        byte[] encryptedBytes = Base64.getDecoder().decode(encrypted);
        encryptedBytes[encryptedBytes.length / 2] ^= 0xFF; // Flip bits
        String tampered = Base64.getEncoder().encodeToString(encryptedBytes);

        assertThrows(
                CryptoException.class,
                () -> {
                    aesGcmService.decryptToString(key, tampered);
                });
    }

    @Test
    @DisplayName("Should fail decryption with truncated ciphertext")
    void testDecryptionWithTruncatedCiphertext() {
        byte[] sharedSecret = new byte[32];
        new java.security.SecureRandom().nextBytes(sharedSecret);
        SecretKey key = aesGcmService.deriveKey(sharedSecret);

        // Ciphertext too short (less than IV + tag)
        String invalidCiphertext = Base64.getEncoder().encodeToString(new byte[10]);

        assertThrows(
                CryptoException.class,
                () -> {
                    aesGcmService.decrypt(key, invalidCiphertext);
                });
    }

    @Test
    @DisplayName("Should handle large payloads")
    void testLargePayload() {
        byte[] sharedSecret = new byte[32];
        new java.security.SecureRandom().nextBytes(sharedSecret);
        SecretKey key = aesGcmService.deriveKey(sharedSecret);

        // Create a 1MB payload
        byte[] largePayload = new byte[1024 * 1024];
        new java.security.SecureRandom().nextBytes(largePayload);

        String encrypted = aesGcmService.encrypt(key, largePayload);
        byte[] decrypted = aesGcmService.decrypt(key, encrypted);

        assertArrayEquals(largePayload, decrypted);
    }

    @Test
    @DisplayName("Should work with ML-KEM derived shared secret")
    void testWithMLKemSharedSecret() {
        // Simulate full ML-KEM + AES-GCM flow
        MLKemService.EncapsulationResult encapsulation = mlKemService.encapsulate();
        byte[] sharedSecret = encapsulation.sharedSecret();

        SecretKey key = aesGcmService.deriveKey(sharedSecret);

        String plaintext = "{\"merkleRoot\": \"abc123\", \"version\": \"1.0.0\"}";
        String encrypted = aesGcmService.encrypt(key, plaintext);

        // Server-side: decapsulate and derive key
        byte[] decapsulatedSecret = mlKemService.decapsulate(encapsulation.encapsulatedKey());
        SecretKey serverKey = aesGcmService.deriveKey(decapsulatedSecret);

        String decrypted = aesGcmService.decryptToString(serverKey, encrypted);

        assertEquals(plaintext, decrypted);
    }
}
