package com.anchorpq.server.service;

import com.anchorpq.server.model.IntegrityPayload;
import com.anchorpq.server.model.IntegrityRecord;
import com.anchorpq.server.model.VerificationResponse;
import com.anchorpq.server.repository.IntegrityRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Optional;

/**
 * Service for verifying application integrity against canonical records.
 *
 * <p>This service contains the core business logic for:
 *
 * <ul>
 *   <li>Comparing client-reported Merkle roots against canonical values
 *   <li>Verifying signer fingerprints
 *   <li>Making integrity decisions
 * </ul>
 */
@ApplicationScoped
public class IntegrityVerificationService {

    @Inject IntegrityRepository integrityRepository;

    /**
     * Verifies the integrity of an application based on the provided payload.
     *
     * <p>Verification process:
     *
     * <ol>
     *   <li>Look up canonical record by version and variant
     *   <li>Compare Merkle roots (constant-time comparison)
     *   <li>Verify signer fingerprint matches
     *   <li>Return appropriate verification status
     * </ol>
     *
     * @param payload The decrypted integrity payload from the client
     * @return VerificationResponse with status and message
     */
    public VerificationResponse verifyIntegrity(IntegrityPayload payload) {
        Log.info(
                "Verifying integrity for version: "
                        + payload.getVersion()
                        + ", variant: "
                        + payload.getVariant());

        // Step 1: Find canonical record
        Optional<IntegrityRecord> canonicalRecord =
                integrityRepository.findByVersionAndVariant(
                        payload.getVersion(), payload.getVariant());

        if (canonicalRecord.isEmpty()) {
            Log.warn(
                    "No canonical record found for version: "
                            + payload.getVersion()
                            + ", variant: "
                            + payload.getVariant());
            return VerificationResponse.rejected(
                    "Unknown application version or variant", "ERR_UNKNOWN_VERSION");
        }

        IntegrityRecord record = canonicalRecord.get();

        // Step 2: Compare Merkle roots (constant-time)
        boolean merkleRootMatch =
                constantTimeEquals(
                        payload.getMerkleRoot().toLowerCase(),
                        record.getMerkleRoot().toLowerCase());

        if (!merkleRootMatch) {
            Log.warn(
                    "Merkle root mismatch for version: "
                            + payload.getVersion()
                            + ", variant: "
                            + payload.getVariant());
            Log.debug(
                    "Expected: "
                            + record.getMerkleRoot().substring(0, 16)
                            + "..., "
                            + "Got: "
                            + payload.getMerkleRoot().substring(0, 16)
                            + "...");
            return VerificationResponse.rejected(
                    "Application integrity verification failed", "ERR_MERKLE_MISMATCH");
        }

        // Step 3: Verify signer fingerprint
        boolean signerMatch =
                constantTimeEquals(
                        payload.getSignerFingerprint().toLowerCase(),
                        record.getSignerFingerprint().toLowerCase());

        if (!signerMatch) {
            Log.warn(
                    "Signer fingerprint mismatch for version: "
                            + payload.getVersion()
                            + ", variant: "
                            + payload.getVariant());
            return VerificationResponse.restricted("Application signed with unknown certificate");
        }

        // All checks passed
        Log.info(
                "Integrity verification APPROVED for version: "
                        + payload.getVersion()
                        + ", variant: "
                        + payload.getVariant());
        return VerificationResponse.approved("Integrity verified successfully");
    }

    /**
     * Performs constant-time string comparison to prevent timing attacks.
     *
     * @param a First string
     * @param b Second string
     * @return true if strings are equal, false otherwise
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    /**
     * Checks if a version/variant combination exists in the database.
     *
     * @param version Application version
     * @param variant Build variant
     * @return true if the record exists
     */
    public boolean isKnownVersion(String version, String variant) {
        return integrityRepository.existsByVersionAndVariant(version, variant);
    }

    /**
     * Registers a new canonical integrity record.
     *
     * @param version Application version
     * @param variant Build variant
     * @param merkleRoot Canonical Merkle root hash
     * @param signerFingerprint Application signer fingerprint
     * @param description Optional description
     * @return The created or updated IntegrityRecord
     */
    @Transactional
    public IntegrityRecord registerIntegrityRecord(
            String version,
            String variant,
            String merkleRoot,
            String signerFingerprint,
            String description) {

        Log.info("Registering integrity record for version: " + version + ", variant: " + variant);

        IntegrityRecord record =
                new IntegrityRecord(version, variant, merkleRoot, signerFingerprint);
        record.setDescription(description);

        return integrityRepository.saveOrUpdate(record);
    }

    /**
     * Retrieves a canonical integrity record.
     *
     * @param version Application version
     * @param variant Build variant
     * @return Optional containing the record if found
     */
    public Optional<IntegrityRecord> getIntegrityRecord(String version, String variant) {
        return integrityRepository.findByVersionAndVariant(version, variant);
    }
}
