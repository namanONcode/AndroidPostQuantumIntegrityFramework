package com.anchorpq.server.repository;

import com.anchorpq.server.model.IntegrityRecord;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

/**
 * Repository for IntegrityRecord entities using Panache.
 *
 * <p>This repository provides database access methods for managing canonical integrity records used
 * for verification.
 */
@ApplicationScoped
public class IntegrityRepository implements PanacheRepository<IntegrityRecord> {

    /**
     * Finds an integrity record by version and variant.
     *
     * @param version Application version string
     * @param variant Build variant (e.g., "release", "debug")
     * @return Optional containing the matching record, or empty if not found
     */
    public Optional<IntegrityRecord> findByVersionAndVariant(String version, String variant) {
        return find("version = ?1 and variant = ?2 and active = true", version, variant)
                .firstResultOptional();
    }

    /**
     * Finds all integrity records for a specific version.
     *
     * @param version Application version string
     * @return List of matching records
     */
    public List<IntegrityRecord> findByVersion(String version) {
        return find("version = ?1 and active = true", version).list();
    }

    /**
     * Finds all integrity records for a specific variant.
     *
     * @param variant Build variant
     * @return List of matching records
     */
    public List<IntegrityRecord> findByVariant(String variant) {
        return find("variant = ?1 and active = true", variant).list();
    }

    /**
     * Finds all active integrity records.
     *
     * @return List of all active records
     */
    public List<IntegrityRecord> findAllActive() {
        return find("active = true").list();
    }

    /**
     * Checks if a record with the given version and variant exists.
     *
     * @param version Application version string
     * @param variant Build variant
     * @return true if a record exists, false otherwise
     */
    public boolean existsByVersionAndVariant(String version, String variant) {
        return count("version = ?1 and variant = ?2", version, variant) > 0;
    }

    /**
     * Deactivates all records for a specific version.
     *
     * @param version Application version string
     * @return Number of records updated
     */
    public int deactivateByVersion(String version) {
        return update("active = false where version = ?1", version);
    }

    /**
     * Finds a record by its Merkle root hash.
     *
     * @param merkleRoot The Merkle root hash in hex format
     * @return Optional containing the matching record, or empty if not found
     */
    public Optional<IntegrityRecord> findByMerkleRoot(String merkleRoot) {
        return find("merkleRoot = ?1 and active = true", merkleRoot.toLowerCase())
                .firstResultOptional();
    }

    /**
     * Counts the number of records matching a signer fingerprint.
     *
     * @param signerFingerprint The signer certificate fingerprint
     * @return Count of matching records
     */
    public long countBySignerFingerprint(String signerFingerprint) {
        return count("signerFingerprint = ?1 and active = true", signerFingerprint.toLowerCase());
    }

    /**
     * Creates or updates an integrity record. If a record with the same version and variant exists,
     * it is updated.
     *
     * @param record The integrity record to save
     * @return The saved record
     */
    public IntegrityRecord saveOrUpdate(IntegrityRecord record) {
        Optional<IntegrityRecord> existing =
                findByVersionAndVariant(record.getVersion(), record.getVariant());

        if (existing.isPresent()) {
            IntegrityRecord existingRecord = existing.get();
            existingRecord.setMerkleRoot(record.getMerkleRoot());
            existingRecord.setSignerFingerprint(record.getSignerFingerprint());
            existingRecord.setDescription(record.getDescription());
            existingRecord.setActive(record.isActive());
            persist(existingRecord);
            return existingRecord;
        } else {
            persist(record);
            return record;
        }
    }
}
