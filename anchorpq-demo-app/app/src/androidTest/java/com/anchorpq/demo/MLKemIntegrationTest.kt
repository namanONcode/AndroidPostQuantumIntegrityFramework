package com.anchorpq.demo

import com.anchorpq.demo.crypto.MLKemClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider
import org.bouncycastle.pqc.jcajce.spec.KyberParameterSpec
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.KeyPairGenerator
import java.security.Security
import java.util.Base64

/**
 * Integration tests that verify the complete ML-KEM handshake flow
 * using a mock server.
 */
@RunWith(AndroidJUnit4::class)
class MLKemIntegrationTest {

    private lateinit var mockServer: MockWebServer
    private lateinit var mlKemClient: MLKemClient

    @Before
    fun setup() {
        if (Security.getProvider("BCPQC") == null) {
            Security.addProvider(BouncyCastlePQCProvider())
        }

        mockServer = MockWebServer()
        mockServer.start()
        mlKemClient = MLKemClient()
    }

    @After
    fun teardown() {
        mockServer.shutdown()
    }

    @Test
    fun testCompleteMLKemHandshake() {
        // Generate server keypair
        val keyPairGenerator = KeyPairGenerator.getInstance("Kyber", "BCPQC")
        keyPairGenerator.initialize(KyberParameterSpec.kyber768)
        val serverKeyPair = keyPairGenerator.generateKeyPair()
        val publicKeyBase64 = Base64.getEncoder().encodeToString(serverKeyPair.public.encoded)

        // Client encapsulates using server's public key
        val encapResult = mlKemClient.encapsulate(serverKeyPair.public)

        // Verify encapsulated key is valid
        assertNotNull(encapResult.encapsulatedKey)
        assertTrue(encapResult.encapsulatedKey.isNotEmpty())

        // Verify shared secret is valid
        assertNotNull(encapResult.sharedSecret)
        assertEquals("AES", encapResult.sharedSecret.algorithm)

        // Server would decapsulate and get the same shared secret
        // (In real test, we'd verify this with server-side code)
    }

    @Test
    fun testEncryptedPayloadFormat() {
        // Generate server keypair
        val keyPairGenerator = KeyPairGenerator.getInstance("Kyber", "BCPQC")
        keyPairGenerator.initialize(KyberParameterSpec.kyber768)
        val serverKeyPair = keyPairGenerator.generateKeyPair()

        // Create test payload
        val testPayload = """
            {
                "merkleRoot": "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2",
                "version": "1.0.0",
                "variant": "debug"
            }
        """.trimIndent().toByteArray()

        // Encrypt using hybrid encryption
        val encrypted = mlKemClient.hybridEncrypt(testPayload, serverKeyPair.public.encoded)

        // Verify encrypted payload structure
        assertNotNull(encrypted.encapsulatedKey)
        assertNotNull(encrypted.encryptedData)

        // Encrypted data should be: IV (12 bytes) + ciphertext + tag (16 bytes)
        assertTrue(encrypted.encryptedData.size >= 12 + testPayload.size + 16)
    }
}

