package com.anchorpq.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Represents the decrypted integrity payload from an Android client.
 *
 * <p>This payload contains the client's computed Merkle root, application version, build variant,
 * and signer fingerprint for verification against the server's canonical records.
 */
public class IntegrityPayload {

    /** The Merkle root hash computed by the client in hexadecimal format. */
    @NotBlank(message = "Merkle root is required")
    @Pattern(
            regexp = "^[a-fA-F0-9]{64}$",
            message = "Merkle root must be a 64-character hex string")
    @JsonProperty("merkleRoot")
    private String merkleRoot;

    /** The application version string. */
    @NotBlank(message = "Version is required")
    @JsonProperty("version")
    private String version;

    /** The build variant (e.g., "release", "debug"). */
    @NotBlank(message = "Variant is required")
    @JsonProperty("variant")
    private String variant;

    /** The application signer certificate fingerprint in hexadecimal format. */
    @NotBlank(message = "Signer fingerprint is required")
    @Pattern(
            regexp = "^[a-fA-F0-9]{64}$",
            message = "Signer fingerprint must be a 64-character hex string")
    @JsonProperty("signerFingerprint")
    private String signerFingerprint;

    public IntegrityPayload() {}

    public IntegrityPayload(
            String merkleRoot, String version, String variant, String signerFingerprint) {
        this.merkleRoot = merkleRoot;
        this.version = version;
        this.variant = variant;
        this.signerFingerprint = signerFingerprint;
    }

    public String getMerkleRoot() {
        return merkleRoot;
    }

    public void setMerkleRoot(String merkleRoot) {
        this.merkleRoot = merkleRoot;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVariant() {
        return variant;
    }

    public void setVariant(String variant) {
        this.variant = variant;
    }

    public String getSignerFingerprint() {
        return signerFingerprint;
    }

    public void setSignerFingerprint(String signerFingerprint) {
        this.signerFingerprint = signerFingerprint;
    }

    @Override
    public String toString() {
        return "IntegrityPayload{"
                + "merkleRoot='"
                + (merkleRoot != null ? merkleRoot.substring(0, 8) + "..." : null)
                + '\''
                + ", version='"
                + version
                + '\''
                + ", variant='"
                + variant
                + '\''
                + ", signerFingerprint='"
                + (signerFingerprint != null ? signerFingerprint.substring(0, 8) + "..." : null)
                + '\''
                + '}';
    }
}
