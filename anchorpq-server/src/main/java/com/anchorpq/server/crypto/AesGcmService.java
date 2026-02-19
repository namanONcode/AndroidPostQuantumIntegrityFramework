package com.anchorpq.server.crypto;

import com.anchorpq.server.config.CryptoConfig;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;

/**
 * Service for symmetric encryption/decryption using AES-256-GCM.
 *
 * <p>This service handles:
 *
 * <ul>
 *   <li>Key derivation from ML-KEM shared secret using HKDF-SHA3
 *   <li>AES-256-GCM encryption and decryption
 * </ul>
 */
@ApplicationScoped
public class AesGcmService {

    private static final String AES_GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final String AES_ALGORITHM = "AES";

    @Inject CryptoConfig cryptoConfig;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Derives an AES key from a shared secret using HKDF with SHA3-256.
     *
     * @param sharedSecret The shared secret from ML-KEM key exchange
     * @param salt Optional salt for HKDF (can be null)
     * @return Derived AES SecretKey
     * @throws CryptoException if key derivation fails
     */
    public SecretKey deriveKey(byte[] sharedSecret, byte[] salt) {
        try {
            int keySize = cryptoConfig.aes().keySize();
            String info = cryptoConfig.hkdf().info();

            // Use SHA3-256 for HKDF
            SHA3Digest sha3Digest = new SHA3Digest(256);
            HKDFBytesGenerator hkdf = new HKDFBytesGenerator(sha3Digest);

            byte[] infoBytes = info.getBytes(StandardCharsets.UTF_8);
            HKDFParameters params =
                    salt != null
                            ? new HKDFParameters(sharedSecret, salt, infoBytes)
                            : HKDFParameters.skipExtractParameters(sharedSecret, infoBytes);

            hkdf.init(params);

            byte[] derivedKey = new byte[keySize / 8];
            hkdf.generateBytes(derivedKey, 0, derivedKey.length);

            Log.debug("Derived AES-" + keySize + " key from shared secret");

            return new SecretKeySpec(derivedKey, AES_ALGORITHM);
        } catch (Exception e) {
            Log.error("Failed to derive AES key", e);
            throw new CryptoException(
                    CryptoException.ErrorCode.KEY_DERIVATION_FAILED,
                    "Failed to derive AES key from shared secret",
                    e);
        }
    }

    /**
     * Derives an AES key from a shared secret without additional salt.
     *
     * @param sharedSecret The shared secret from ML-KEM key exchange
     * @return Derived AES SecretKey
     */
    public SecretKey deriveKey(byte[] sharedSecret) {
        return deriveKey(sharedSecret, null);
    }

    /**
     * Encrypts plaintext using AES-256-GCM.
     *
     * <p>The output format is: IV (12 bytes) || Ciphertext || Auth Tag (16 bytes)
     *
     * @param key The AES secret key
     * @param plaintext The plaintext to encrypt
     * @return Base64-encoded encrypted data (IV + ciphertext + tag)
     * @throws CryptoException if encryption fails
     */
    public String encrypt(SecretKey key, byte[] plaintext) {
        try {
            int ivSize = cryptoConfig.aes().ivSize();
            int tagSize = cryptoConfig.aes().tagSize();

            // Generate random IV
            byte[] iv = new byte[ivSize];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(tagSize, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

            byte[] ciphertext = cipher.doFinal(plaintext);

            // Concatenate IV + ciphertext (which includes auth tag)
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            Log.error("Failed to encrypt data", e);
            throw new CryptoException(
                    CryptoException.ErrorCode.ENCRYPTION_FAILED, "Failed to encrypt data", e);
        }
    }

    /**
     * Encrypts a string using AES-256-GCM.
     *
     * @param key The AES secret key
     * @param plaintext The string to encrypt
     * @return Base64-encoded encrypted data
     */
    public String encrypt(SecretKey key, String plaintext) {
        return encrypt(key, plaintext.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decrypts AES-256-GCM encrypted data.
     *
     * <p>Expected input format: IV (12 bytes) || Ciphertext || Auth Tag (16 bytes)
     *
     * @param key The AES secret key
     * @param encryptedDataBase64 Base64-encoded encrypted data
     * @return Decrypted plaintext bytes
     * @throws CryptoException if decryption fails or authentication tag is invalid
     */
    public byte[] decrypt(SecretKey key, String encryptedDataBase64) {
        try {
            int ivSize = cryptoConfig.aes().ivSize();
            int tagSize = cryptoConfig.aes().tagSize();

            byte[] encryptedData = Base64.getDecoder().decode(encryptedDataBase64);

            if (encryptedData.length < ivSize + (tagSize / 8)) {
                throw new CryptoException(
                        CryptoException.ErrorCode.INVALID_CIPHERTEXT, "Encrypted data too short");
            }

            // Extract IV and ciphertext
            byte[] iv = Arrays.copyOfRange(encryptedData, 0, ivSize);
            byte[] ciphertext = Arrays.copyOfRange(encryptedData, ivSize, encryptedData.length);

            Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(tagSize, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

            byte[] plaintext = cipher.doFinal(ciphertext);

            Log.debug("Successfully decrypted " + plaintext.length + " bytes");

            return plaintext;
        } catch (CryptoException e) {
            throw e;
        } catch (javax.crypto.AEADBadTagException e) {
            Log.warn("Authentication tag verification failed - potential tampering detected");
            throw new CryptoException(
                    CryptoException.ErrorCode.AUTHENTICATION_FAILED,
                    "Authentication tag verification failed",
                    e);
        } catch (Exception e) {
            Log.error("Failed to decrypt data", e);
            throw new CryptoException(
                    CryptoException.ErrorCode.DECRYPTION_FAILED, "Failed to decrypt data", e);
        }
    }

    /**
     * Decrypts AES-256-GCM encrypted data and returns as string.
     *
     * @param key The AES secret key
     * @param encryptedDataBase64 Base64-encoded encrypted data
     * @return Decrypted plaintext string
     */
    public String decryptToString(SecretKey key, String encryptedDataBase64) {
        byte[] plaintext = decrypt(key, encryptedDataBase64);
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    /**
     * Generates a random IV for AES-GCM encryption.
     *
     * @return Random IV bytes
     */
    public byte[] generateIv() {
        byte[] iv = new byte[cryptoConfig.aes().ivSize()];
        secureRandom.nextBytes(iv);
        return iv;
    }
}
