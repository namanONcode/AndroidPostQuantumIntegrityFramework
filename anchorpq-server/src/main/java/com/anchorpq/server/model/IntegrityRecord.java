package com.anchorpq.server.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Objects;

/**
 * Entity representing a canonical integrity record for an application version.
 *
 * <p>This entity stores the authoritative Merkle root hash for a specific application version and
 * build variant, allowing server-side verification of client-reported integrity payloads.
 */
@Entity
@Table(
        name = "integrity_records",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_version_variant",
                        columnNames = {"version", "variant"}),
        indexes = {
            @Index(name = "idx_version_variant", columnList = "version, variant"),
            @Index(name = "idx_created_at", columnList = "created_at")
        })
public class IntegrityRecord extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Application version string (e.g., "1.0.0", "2.1.3-beta"). */
    @NotBlank(message = "Version is required")
    @Size(max = 50, message = "Version must not exceed 50 characters")
    @Column(name = "version", nullable = false, length = 50)
    private String version;

    /** Build variant (e.g., "release", "debug", "staging"). */
    @NotBlank(message = "Variant is required")
    @Size(max = 30, message = "Variant must not exceed 30 characters")
    @Column(name = "variant", nullable = false, length = 30)
    private String variant;

    /** Canonical Merkle root hash in hexadecimal format. */
    @NotBlank(message = "Merkle root is required")
    @Pattern(
            regexp = "^[a-fA-F0-9]{64}$",
            message = "Merkle root must be a 64-character hex string (SHA-256)")
    @Column(name = "merkle_root", nullable = false, length = 64)
    private String merkleRoot;

    /** Application signer fingerprint in hexadecimal format. */
    @NotBlank(message = "Signer fingerprint is required")
    @Pattern(
            regexp = "^[a-fA-F0-9]{64}$",
            message = "Signer fingerprint must be a 64-character hex string")
    @Column(name = "signer_fingerprint", nullable = false, length = 64)
    private String signerFingerprint;

    /** Timestamp when this record was created. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Timestamp when this record was last updated. */
    @Column(name = "updated_at")
    private Instant updatedAt;

    /** Optional description or notes for this record. */
    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Column(name = "description", length = 500)
    private String description;

    /** Flag indicating if this record is active. */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    public IntegrityRecord() {
        // Default constructor for JPA
    }

    public IntegrityRecord(
            String version, String variant, String merkleRoot, String signerFingerprint) {
        this.version = version;
        this.variant = variant;
        this.merkleRoot = merkleRoot;
        this.signerFingerprint = signerFingerprint;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getMerkleRoot() {
        return merkleRoot;
    }

    public void setMerkleRoot(String merkleRoot) {
        this.merkleRoot = merkleRoot;
    }

    public String getSignerFingerprint() {
        return signerFingerprint;
    }

    public void setSignerFingerprint(String signerFingerprint) {
        this.signerFingerprint = signerFingerprint;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntegrityRecord that = (IntegrityRecord) o;
        return Objects.equals(version, that.version) && Objects.equals(variant, that.variant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, variant);
    }

    @Override
    public String toString() {
        return "IntegrityRecord{"
                + "id="
                + id
                + ", version='"
                + version
                + '\''
                + ", variant='"
                + variant
                + '\''
                + ", merkleRoot='"
                + (merkleRoot != null ? merkleRoot.substring(0, 8) + "..." : null)
                + '\''
                + ", active="
                + active
                + ", createdAt="
                + createdAt
                + '}';
    }
}
