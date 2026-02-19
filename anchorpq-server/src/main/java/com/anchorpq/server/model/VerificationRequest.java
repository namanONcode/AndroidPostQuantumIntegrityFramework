package com.anchorpq.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the encrypted verification request from an Android client.
 *
 * <p>This request contains the ML-KEM encapsulated ciphertext (for key exchange) and the AES-GCM
 * encrypted integrity payload.
 */
public class VerificationRequest {

    /**
     * The ML-KEM encapsulated ciphertext (encapsulation of shared secret). Base64-encoded bytes.
     */
    @JsonProperty("encapsulatedKey")
    private String encapsulatedKey;

    /**
     * The AES-256-GCM encrypted integrity payload. Base64-encoded bytes containing IV + ciphertext
     * + auth tag.
     */
    @JsonProperty("encryptedPayload")
    private String encryptedPayload;

    /** Optional nonce/timestamp for replay protection. */
    @JsonProperty("nonce")
    private String nonce;

    /** Client timestamp in milliseconds since epoch. */
    @JsonProperty("timestamp")
    private Long timestamp;

    public VerificationRequest() {}

    public VerificationRequest(String encapsulatedKey, String encryptedPayload) {
        this.encapsulatedKey = encapsulatedKey;
        this.encryptedPayload = encryptedPayload;
    }

    public String getEncapsulatedKey() {
        return encapsulatedKey;
    }

    public void setEncapsulatedKey(String encapsulatedKey) {
        this.encapsulatedKey = encapsulatedKey;
    }

    public String getEncryptedPayload() {
        return encryptedPayload;
    }

    public void setEncryptedPayload(String encryptedPayload) {
        this.encryptedPayload = encryptedPayload;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "VerificationRequest{"
                + "encapsulatedKey='"
                + (encapsulatedKey != null
                        ? encapsulatedKey.substring(0, Math.min(16, encapsulatedKey.length()))
                                + "..."
                        : null)
                + '\''
                + ", encryptedPayload='"
                + (encryptedPayload != null
                        ? encryptedPayload.substring(0, Math.min(16, encryptedPayload.length()))
                                + "..."
                        : null)
                + '\''
                + ", nonce='"
                + nonce
                + '\''
                + ", timestamp="
                + timestamp
                + '}';
    }
}
