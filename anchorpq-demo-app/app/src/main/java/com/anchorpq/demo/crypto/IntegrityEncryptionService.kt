package com.anchorpq.demo.crypto

import android.os.Build
import android.util.Base64
import com.anchorpq.demo.model.DeviceInfo
import com.anchorpq.demo.model.IntegrityPayload
import com.anchorpq.demo.model.VerificationRequest
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.time.Instant

/**
 * Service for creating encrypted integrity verification requests.
 *
 * This class orchestrates:
 * 1. Building the integrity payload
 * 2. Encrypting with ML-KEM + AES-GCM
 * 3. Creating the verification request
 */
class IntegrityEncryptionService {

    private val mlKemClient = MLKemClient()
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()
    private val secureRandom = SecureRandom()

    /**
     * Creates an encrypted verification request.
     *
     * @param merkleRoot The application's Merkle root hash
     * @param version Application version
     * @param variant Build variant (debug/release)
     * @param serverPublicKeyBytes Server's ML-KEM public key
     * @param signerFingerprint Optional signing certificate fingerprint
     * @return VerificationRequest ready to be sent to the server
     * @throws GeneralSecurityException if encryption fails
     */
    @Throws(GeneralSecurityException::class)
    fun createVerificationRequest(
        merkleRoot: String,
        version: String,
        variant: String,
        serverPublicKeyBytes: ByteArray,
        signerFingerprint: String? = null
    ): VerificationRequest {
        // Build the integrity payload
        val payload = buildIntegrityPayload(merkleRoot, version, variant, signerFingerprint)

        // Serialize to JSON
        val payloadJson = gson.toJson(payload)
        val payloadBytes = payloadJson.toByteArray(Charsets.UTF_8)

        // Encrypt with ML-KEM + AES-GCM
        val encrypted = mlKemClient.hybridEncrypt(payloadBytes, serverPublicKeyBytes)

        // Create the verification request
        return VerificationRequest(
            encapsulatedKey = encodeBase64(encrypted.encapsulatedKey),
            encryptedPayload = encodeBase64(encrypted.encryptedData),
            timestamp = System.currentTimeMillis(),
            nonce = generateNonce()
        )
    }

    /**
     * Builds the integrity payload to be encrypted.
     */
    private fun buildIntegrityPayload(
        merkleRoot: String,
        version: String,
        variant: String,
        signerFingerprint: String?
    ): IntegrityPayload {
        return IntegrityPayload(
            merkleRoot = merkleRoot,
            version = version,
            variant = variant,
            timestamp = Instant.now().toString(),
            nonce = generateNonce(),
            signerFingerprint = signerFingerprint,
            device = DeviceInfo(
                sdk = Build.VERSION.SDK_INT.toString(),
                model = Build.MODEL,
                manufacturer = Build.MANUFACTURER
            )
        )
    }

    /**
     * Helper to encode bytes to Base64 string.
     * Uses java.util.Base64 on API 26+ for standard compliance,
     * falls back to android.util.Base64 on older devices.
     */
    private fun encodeBase64(bytes: ByteArray): String {
        return if (Build.VERSION.SDK_INT >= 26) {
            java.util.Base64.getEncoder().encodeToString(bytes)
        } else {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
    }

    /**
     * Generates a random nonce for replay protection.
     */
    private fun generateNonce(): String {
        val nonce = ByteArray(16)
        secureRandom.nextBytes(nonce)
        return encodeBase64(nonce)
    }
}

