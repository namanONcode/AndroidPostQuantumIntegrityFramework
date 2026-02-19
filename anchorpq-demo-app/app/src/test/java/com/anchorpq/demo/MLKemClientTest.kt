package com.anchorpq.demo

import com.anchorpq.demo.crypto.MLKemClient
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Security

/**
 * Unit tests for ML-KEM cryptographic operations.
 * Tests encapsulation and encryption without server interaction.
 */
class MLKemClientTest {

    private lateinit var mlKemClient: MLKemClient

    @Before
    fun setup() {
        // Ensure Bouncy Castle provider is registered
        if (Security.getProvider("BCPQC") == null) {
            Security.addProvider(BouncyCastlePQCProvider())
        }
        mlKemClient = MLKemClient()
    }

    @Test
    fun `test encapsulation produces valid output`() {
        // Generate a test keypair (simulating server)
        val keyPairGenerator = KeyPairGenerator.getInstance("Kyber", "BCPQC")
        keyPairGenerator.initialize(org.bouncycastle.pqc.jcajce.spec.KyberParameterSpec.kyber768)
        val keyPair = keyPairGenerator.generateKeyPair()

        // Perform encapsulation
        val result = mlKemClient.encapsulate(keyPair.public)

        // Verify output
        assertNotNull(result.encapsulatedKey)
        assertNotNull(result.sharedSecret)
        assertTrue(result.encapsulatedKey.isNotEmpty())
        assertEquals("AES", result.sharedSecret.algorithm)
    }

    @Test
    fun `test encryption produces valid ciphertext`() {
        // Generate a test keypair
        val keyPairGenerator = KeyPairGenerator.getInstance("Kyber", "BCPQC")
        keyPairGenerator.initialize(org.bouncycastle.pqc.jcajce.spec.KyberParameterSpec.kyber768)
        val keyPair = keyPairGenerator.generateKeyPair()

        // Encapsulate
        val encapResult = mlKemClient.encapsulate(keyPair.public)

        // Encrypt test data
        val plaintext = "Test integrity payload".toByteArray()
        val ciphertext = mlKemClient.encrypt(plaintext, encapResult.sharedSecret)

        // Verify ciphertext
        assertNotNull(ciphertext)
        assertTrue(ciphertext.size > plaintext.size) // IV + tag overhead
        assertFalse(ciphertext.contentEquals(plaintext))
    }

    @Test
    fun `test hybrid encryption produces valid payload`() {
        // Generate a test keypair
        val keyPairGenerator = KeyPairGenerator.getInstance("Kyber", "BCPQC")
        keyPairGenerator.initialize(org.bouncycastle.pqc.jcajce.spec.KyberParameterSpec.kyber768)
        val keyPair = keyPairGenerator.generateKeyPair()

        // Perform hybrid encryption
        val plaintext = """{"merkleRoot":"abc123","version":"1.0.0"}""".toByteArray()
        val payload = mlKemClient.hybridEncrypt(plaintext, keyPair.public.encoded)

        // Verify payload
        assertNotNull(payload.encapsulatedKey)
        assertNotNull(payload.encryptedData)
        assertTrue(payload.encapsulatedKey.isNotEmpty())
        assertTrue(payload.encryptedData.isNotEmpty())
    }

    @Test
    fun `test different encapsulations produce different shared secrets`() {
        // Generate a test keypair
        val keyPairGenerator = KeyPairGenerator.getInstance("Kyber", "BCPQC")
        keyPairGenerator.initialize(org.bouncycastle.pqc.jcajce.spec.KyberParameterSpec.kyber768)
        val keyPair = keyPairGenerator.generateKeyPair()

        // Perform two encapsulations
        val result1 = mlKemClient.encapsulate(keyPair.public)
        val result2 = mlKemClient.encapsulate(keyPair.public)

        // Verify different outputs (due to randomness)
        assertFalse(result1.encapsulatedKey.contentEquals(result2.encapsulatedKey))
    }

    @Test
    fun `test encryption with different shared secrets produces different ciphertext`() {
        // Generate a test keypair
        val keyPairGenerator = KeyPairGenerator.getInstance("Kyber", "BCPQC")
        keyPairGenerator.initialize(org.bouncycastle.pqc.jcajce.spec.KyberParameterSpec.kyber768)
        val keyPair = keyPairGenerator.generateKeyPair()

        val plaintext = "Same plaintext".toByteArray()

        // Two encapsulations with different shared secrets
        val encap1 = mlKemClient.encapsulate(keyPair.public)
        val encap2 = mlKemClient.encapsulate(keyPair.public)

        val ciphertext1 = mlKemClient.encrypt(plaintext, encap1.sharedSecret)
        val ciphertext2 = mlKemClient.encrypt(plaintext, encap2.sharedSecret)

        // Different ciphertexts
        assertFalse(ciphertext1.contentEquals(ciphertext2))
    }
}

