package com.anchorpq.demo.crypto

import org.bouncycastle.jcajce.SecretKeyWithEncapsulation
import org.bouncycastle.jcajce.spec.KEMGenerateSpec
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider
import org.bouncycastle.pqc.jcajce.spec.KyberParameterSpec
import java.security.*
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * ML-KEM (CRYSTALS-Kyber) key encapsulation mechanism helper.
 *
 * This class provides:
 * - Key encapsulation using server's public key
 * - AES-GCM encryption using the shared secret
 * - Hybrid encryption combining ML-KEM + AES-GCM
 *
 * Security Level: Kyber-768 (equivalent to AES-192)
 */
class MLKemClient {

    companion object {
        private const val KEM_ALGORITHM = "Kyber"
        private const val PROVIDER = "BCPQC"
        private const val AES_ALGORITHM = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128 // bits
        private const val GCM_NONCE_LENGTH = 12 // bytes

        init {
            // Register Bouncy Castle PQC provider
            if (Security.getProvider(PROVIDER) == null) {
                Security.addProvider(BouncyCastlePQCProvider())
            }
        }
    }

    private val secureRandom = SecureRandom()

    /**
     * Encapsulates a shared secret using the server's public key.
     *
     * @param serverPublicKeyBytes The server's ML-KEM public key (X.509 encoded)
     * @return EncapsulationResult containing the encapsulated key and shared secret
     * @throws GeneralSecurityException if encapsulation fails
     */
    @Throws(GeneralSecurityException::class)
    fun encapsulate(serverPublicKeyBytes: ByteArray): EncapsulationResult {
        // Decode the server's public key
        val keyFactory = KeyFactory.getInstance(KEM_ALGORITHM, PROVIDER)
        val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(serverPublicKeyBytes))

        return encapsulate(publicKey)
    }

    /**
     * Encapsulates a shared secret using the server's public key.
     *
     * @param serverPublicKey The server's ML-KEM public key
     * @return EncapsulationResult containing the encapsulated key and shared secret
     * @throws GeneralSecurityException if encapsulation fails
     */
    @Throws(GeneralSecurityException::class)
    fun encapsulate(serverPublicKey: PublicKey): EncapsulationResult {
        val keyGenerator = KeyGenerator.getInstance(KEM_ALGORITHM, PROVIDER)
        keyGenerator.init(KEMGenerateSpec(serverPublicKey, "AES"), secureRandom)

        val secretKey = keyGenerator.generateKey() as SecretKeyWithEncapsulation

        return EncapsulationResult(
            encapsulatedKey = secretKey.encapsulation,
            sharedSecret = secretKey
        )
    }

    /**
     * Encrypts data using AES-256-GCM with the shared secret.
     *
     * @param plaintext The data to encrypt
     * @param sharedSecret The shared secret from encapsulation
     * @return Encrypted data with prepended nonce (IV || ciphertext || auth tag)
     * @throws GeneralSecurityException if encryption fails
     */
    @Throws(GeneralSecurityException::class)
    fun encrypt(plaintext: ByteArray, sharedSecret: SecretKey): ByteArray {
        // Generate random nonce
        val nonce = ByteArray(GCM_NONCE_LENGTH)
        secureRandom.nextBytes(nonce)

        // Initialize cipher
        val cipher = Cipher.getInstance(AES_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, sharedSecret, GCMParameterSpec(GCM_TAG_LENGTH, nonce))

        val ciphertext = cipher.doFinal(plaintext)

        // Prepend nonce to ciphertext
        val result = ByteArray(nonce.size + ciphertext.size)
        System.arraycopy(nonce, 0, result, 0, nonce.size)
        System.arraycopy(ciphertext, 0, result, nonce.size, ciphertext.size)

        return result
    }

    /**
     * Performs hybrid encryption: ML-KEM key exchange + AES-GCM encryption.
     *
     * This is the main method used for encrypting integrity payloads.
     *
     * @param plaintext The data to encrypt
     * @param serverPublicKeyBytes The server's ML-KEM public key
     * @return HybridEncryptedPayload containing encapsulated key and encrypted data
     * @throws GeneralSecurityException if encryption fails
     */
    @Throws(GeneralSecurityException::class)
    fun hybridEncrypt(plaintext: ByteArray, serverPublicKeyBytes: ByteArray): HybridEncryptedPayload {
        // Encapsulate shared secret
        val encapResult = encapsulate(serverPublicKeyBytes)

        // Encrypt with shared secret
        val encryptedData = encrypt(plaintext, encapResult.sharedSecret)

        return HybridEncryptedPayload(
            encapsulatedKey = encapResult.encapsulatedKey,
            encryptedData = encryptedData
        )
    }

    /**
     * Result of ML-KEM key encapsulation.
     */
    data class EncapsulationResult(
        /** The encapsulated key (ciphertext) to send to the server */
        val encapsulatedKey: ByteArray,
        /** The shared secret for symmetric encryption */
        val sharedSecret: SecretKey
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as EncapsulationResult
            return encapsulatedKey.contentEquals(other.encapsulatedKey)
        }

        override fun hashCode(): Int = encapsulatedKey.contentHashCode()
    }

    /**
     * Result of hybrid encryption (ML-KEM + AES-GCM).
     */
    data class HybridEncryptedPayload(
        /** The ML-KEM encapsulated key */
        val encapsulatedKey: ByteArray,
        /** The AES-GCM encrypted data (IV || ciphertext || auth tag) */
        val encryptedData: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as HybridEncryptedPayload
            return encapsulatedKey.contentEquals(other.encapsulatedKey) &&
                   encryptedData.contentEquals(other.encryptedData)
        }

        override fun hashCode(): Int {
            var result = encapsulatedKey.contentHashCode()
            result = 31 * result + encryptedData.contentHashCode()
            return result
        }
    }
}

