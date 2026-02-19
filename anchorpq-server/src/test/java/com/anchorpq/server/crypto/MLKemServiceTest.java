package com.anchorpq.server.crypto;

import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for ML-KEM cryptographic operations. */
@QuarkusTest
class MLKemServiceTest {

    @Inject MLKemService mlKemService;

    @Test
    @DisplayName("Should generate and expose public key")
    void testPublicKeyGeneration() {
        String publicKeyBase64 = mlKemService.getPublicKeyBase64();

        assertNotNull(publicKeyBase64);
        assertFalse(publicKeyBase64.isEmpty());

        // ML-KEM-768 public key should be substantial (around 1184 bytes encoded)
        assertTrue(publicKeyBase64.length() > 1000);
    }

    @Test
    @DisplayName("Should have valid key ID and timestamp")
    void testKeyMetadata() {
        String keyId = mlKemService.getKeyId();
        long generatedAt = mlKemService.getKeyGeneratedAt();
        String parameterSet = mlKemService.getParameterSet();

        assertNotNull(keyId);
        assertFalse(keyId.isEmpty());
        assertTrue(generatedAt > 0);
        assertNotNull(parameterSet);
    }

    @Test
    @DisplayName("Should successfully encapsulate and decapsulate shared secret")
    void testEncapsulationDecapsulation() {
        // Perform encapsulation (simulating client)
        MLKemService.EncapsulationResult encapsulation = mlKemService.encapsulate();

        assertNotNull(encapsulation);
        assertNotNull(encapsulation.encapsulatedKey());
        assertNotNull(encapsulation.sharedSecret());
        assertTrue(encapsulation.encapsulatedKey().length > 0);
        assertTrue(encapsulation.sharedSecret().length > 0);

        // Perform decapsulation (server-side)
        byte[] decapsulatedSecret = mlKemService.decapsulate(encapsulation.encapsulatedKey());

        assertNotNull(decapsulatedSecret);
        assertArrayEquals(
                encapsulation.sharedSecret(),
                decapsulatedSecret,
                "Decapsulated secret should match encapsulated secret");
    }

    @Test
    @DisplayName("Should handle Base64 encoded encapsulated key")
    void testBase64Decapsulation() {
        // Encapsulate
        MLKemService.EncapsulationResult encapsulation = mlKemService.encapsulate();
        String encapsulatedKeyBase64 = encapsulation.getEncapsulatedKeyBase64();

        // Decapsulate using Base64 string
        byte[] decapsulatedSecret = mlKemService.decapsulate(encapsulatedKeyBase64);

        assertArrayEquals(encapsulation.sharedSecret(), decapsulatedSecret);
    }

    @Test
    @DisplayName("Should throw exception for invalid encapsulated key")
    void testInvalidEncapsulatedKey() {
        byte[] invalidKey = new byte[100]; // Too short / invalid format

        // The exception can be CryptoException or IllegalArgumentException depending on
        // where the validation fails in the Bouncy Castle library
        assertThrows(
                Exception.class,
                () -> {
                    mlKemService.decapsulate(invalidKey);
                });
    }

    @Test
    @DisplayName("Should produce different shared secrets for different encapsulations")
    void testEncapsulationUniqueness() {
        MLKemService.EncapsulationResult encap1 = mlKemService.encapsulate();
        MLKemService.EncapsulationResult encap2 = mlKemService.encapsulate();

        // Each encapsulation should produce different results
        assertFalse(
                java.util.Arrays.equals(encap1.sharedSecret(), encap2.sharedSecret()),
                "Different encapsulations should produce different shared secrets");
        assertFalse(
                java.util.Arrays.equals(encap1.encapsulatedKey(), encap2.encapsulatedKey()),
                "Different encapsulations should produce different ciphertexts");
    }
}
