package com.anchorpq.server.crypto;

import com.anchorpq.server.model.IntegrityPayload;
import com.anchorpq.server.model.VerificationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.crypto.SecretKey;

/**
 * High-level cryptographic service that orchestrates the complete post-quantum secure decryption
 * flow for integrity verification.
 *
 * <p>This service coordinates:
 *
 * <ol>
 *   <li>ML-KEM shared secret decapsulation
 *   <li>HKDF key derivation
 *   <li>AES-256-GCM payload decryption
 *   <li>JSON deserialization of integrity payload
 * </ol>
 */
@ApplicationScoped
public class CryptoService {

    @Inject MLKemService mlKemService;

    @Inject AesGcmService aesGcmService;

    @Inject ObjectMapper objectMapper;

    /**
     * Decrypts and deserializes an integrity verification request.
     *
     * <p>This method performs the complete post-quantum secure decryption flow:
     *
     * <ol>
     *   <li>Decapsulates the shared secret using ML-KEM private key
     *   <li>Derives an AES-256 key from the shared secret using HKDF-SHA3
     *   <li>Decrypts the payload using AES-256-GCM
     *   <li>Deserializes the JSON payload into an IntegrityPayload object
     * </ol>
     *
     * @param request The encrypted verification request from the client
     * @return Decrypted and deserialized IntegrityPayload
     * @throws CryptoException if any cryptographic operation fails
     * @throws IllegalArgumentException if the request is invalid
     */
    public IntegrityPayload decryptVerificationRequest(VerificationRequest request) {
        validateRequest(request);

        Log.debug("Starting decryption of verification request");

        try {
            // Step 1: Decapsulate shared secret using ML-KEM
            Log.debug("Decapsulating shared secret...");
            byte[] sharedSecret = mlKemService.decapsulate(request.getEncapsulatedKey());

            // Step 2: Derive AES key from shared secret using HKDF
            Log.debug("Deriving AES key from shared secret...");
            SecretKey aesKey = aesGcmService.deriveKey(sharedSecret);

            // Step 3: Decrypt the payload using AES-GCM
            Log.debug("Decrypting integrity payload...");
            String payloadJson =
                    aesGcmService.decryptToString(aesKey, request.getEncryptedPayload());

            // Step 4: Deserialize JSON to IntegrityPayload
            Log.debug("Deserializing integrity payload...");
            IntegrityPayload payload = objectMapper.readValue(payloadJson, IntegrityPayload.class);

            Log.info(
                    "Successfully decrypted verification request for version: "
                            + payload.getVersion()
                            + ", variant: "
                            + payload.getVariant());

            return payload;

        } catch (CryptoException e) {
            // Re-throw crypto exceptions with original error code
            throw e;
        } catch (Exception e) {
            Log.error("Failed to decrypt verification request", e);
            throw new CryptoException(
                    CryptoException.ErrorCode.DECRYPTION_FAILED,
                    "Failed to process verification request: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Creates an encrypted verification request for testing purposes.
     *
     * <p>This method simulates a client encrypting an integrity payload:
     *
     * <ol>
     *   <li>Encapsulates a shared secret using the server's public key
     *   <li>Derives an AES-256 key from the shared secret
     *   <li>Encrypts the payload using AES-256-GCM
     * </ol>
     *
     * @param payload The integrity payload to encrypt
     * @return Encrypted VerificationRequest
     * @throws CryptoException if encryption fails
     */
    public VerificationRequest createEncryptedRequest(IntegrityPayload payload) {
        try {
            // Serialize payload to JSON
            String payloadJson = objectMapper.writeValueAsString(payload);

            // Encapsulate shared secret
            MLKemService.EncapsulationResult encapsulation = mlKemService.encapsulate();

            // Derive AES key
            SecretKey aesKey = aesGcmService.deriveKey(encapsulation.sharedSecret());

            // Encrypt payload
            String encryptedPayload = aesGcmService.encrypt(aesKey, payloadJson);

            // Create request
            VerificationRequest request = new VerificationRequest();
            request.setEncapsulatedKey(encapsulation.getEncapsulatedKeyBase64());
            request.setEncryptedPayload(encryptedPayload);
            request.setTimestamp(System.currentTimeMillis());

            return request;

        } catch (Exception e) {
            Log.error("Failed to create encrypted request", e);
            throw new CryptoException(
                    CryptoException.ErrorCode.ENCRYPTION_FAILED,
                    "Failed to create encrypted request: " + e.getMessage(),
                    e);
        }
    }

    /** Validates that the verification request contains required fields. */
    private void validateRequest(VerificationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Verification request cannot be null");
        }
        if (request.getEncapsulatedKey() == null || request.getEncapsulatedKey().isEmpty()) {
            throw new IllegalArgumentException("Encapsulated key is required");
        }
        if (request.getEncryptedPayload() == null || request.getEncryptedPayload().isEmpty()) {
            throw new IllegalArgumentException("Encrypted payload is required");
        }
    }
}
