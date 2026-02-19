package com.anchorpq.server.service;

import static org.junit.jupiter.api.Assertions.*;

import com.anchorpq.server.model.IntegrityPayload;
import com.anchorpq.server.model.IntegrityRecord;
import com.anchorpq.server.model.VerificationResponse;
import com.anchorpq.server.repository.IntegrityRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;

/** Unit tests for integrity verification service. */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegrityVerificationServiceTest {

    @Inject IntegrityVerificationService verificationService;

    @Inject IntegrityRepository integrityRepository;

    private static final String TEST_VERSION = "1.0.0";
    private static final String TEST_VARIANT = "release";
    private static final String TEST_MERKLE_ROOT =
            "a1b2c3d4e5f6789012345678901234567890123456789012345678901234abcd";
    private static final String TEST_SIGNER_FP =
            "fedcba0987654321fedcba0987654321fedcba0987654321fedcba09876543fe";

    @BeforeEach
    @Transactional
    void setup() {
        // Clean up any existing test data
        integrityRepository.delete("version = ?1 and variant = ?2", TEST_VERSION, TEST_VARIANT);

        // Create test record
        IntegrityRecord record =
                new IntegrityRecord(TEST_VERSION, TEST_VARIANT, TEST_MERKLE_ROOT, TEST_SIGNER_FP);
        record.setDescription("Test record");
        integrityRepository.persist(record);
    }

    @AfterEach
    @Transactional
    void cleanup() {
        integrityRepository.delete("version = ?1", TEST_VERSION);
    }

    @Test
    @Order(1)
    @DisplayName("Should approve matching integrity payload")
    void testMatchingIntegrity() {
        IntegrityPayload payload =
                new IntegrityPayload(TEST_MERKLE_ROOT, TEST_VERSION, TEST_VARIANT, TEST_SIGNER_FP);

        VerificationResponse response = verificationService.verifyIntegrity(payload);

        assertEquals(VerificationResponse.Status.APPROVED, response.getStatus());
        assertNotNull(response.getMessage());
    }

    @Test
    @Order(2)
    @DisplayName("Should reject mismatched Merkle root")
    void testMismatchedMerkleRoot() {
        IntegrityPayload payload =
                new IntegrityPayload(
                        "0000000000000000000000000000000000000000000000000000000000000000", // Wrong
                        // root
                        TEST_VERSION,
                        TEST_VARIANT,
                        TEST_SIGNER_FP);

        VerificationResponse response = verificationService.verifyIntegrity(payload);

        assertEquals(VerificationResponse.Status.REJECTED, response.getStatus());
        assertEquals("ERR_MERKLE_MISMATCH", response.getErrorCode());
    }

    @Test
    @Order(3)
    @DisplayName("Should restrict for mismatched signer fingerprint")
    void testMismatchedSignerFingerprint() {
        IntegrityPayload payload =
                new IntegrityPayload(
                        TEST_MERKLE_ROOT,
                        TEST_VERSION,
                        TEST_VARIANT,
                        "0000000000000000000000000000000000000000000000000000000000000000" // Wrong
                        // signer
                        );

        VerificationResponse response = verificationService.verifyIntegrity(payload);

        assertEquals(VerificationResponse.Status.RESTRICTED, response.getStatus());
    }

    @Test
    @Order(4)
    @DisplayName("Should reject unknown version")
    void testUnknownVersion() {
        IntegrityPayload payload =
                new IntegrityPayload(
                        TEST_MERKLE_ROOT,
                        "99.99.99", // Unknown version
                        TEST_VARIANT,
                        TEST_SIGNER_FP);

        VerificationResponse response = verificationService.verifyIntegrity(payload);

        assertEquals(VerificationResponse.Status.REJECTED, response.getStatus());
        assertEquals("ERR_UNKNOWN_VERSION", response.getErrorCode());
    }

    @Test
    @Order(5)
    @DisplayName("Should reject unknown variant")
    void testUnknownVariant() {
        IntegrityPayload payload =
                new IntegrityPayload(
                        TEST_MERKLE_ROOT,
                        TEST_VERSION,
                        "unknown_variant", // Unknown variant
                        TEST_SIGNER_FP);

        VerificationResponse response = verificationService.verifyIntegrity(payload);

        assertEquals(VerificationResponse.Status.REJECTED, response.getStatus());
        assertEquals("ERR_UNKNOWN_VERSION", response.getErrorCode());
    }

    @Test
    @Order(6)
    @DisplayName("Should handle case-insensitive Merkle root comparison")
    void testCaseInsensitiveMerkleRoot() {
        IntegrityPayload payload =
                new IntegrityPayload(
                        TEST_MERKLE_ROOT.toUpperCase(), // Uppercase
                        TEST_VERSION,
                        TEST_VARIANT,
                        TEST_SIGNER_FP);

        VerificationResponse response = verificationService.verifyIntegrity(payload);

        assertEquals(VerificationResponse.Status.APPROVED, response.getStatus());
    }

    @Test
    @Order(7)
    @DisplayName("Should register new integrity record")
    @Transactional
    void testRegisterIntegrityRecord() {
        String newVersion = "2.0.0";
        String newVariant = "debug";
        String newMerkleRoot = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
        String newSignerFp = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";

        IntegrityRecord record =
                verificationService.registerIntegrityRecord(
                        newVersion, newVariant, newMerkleRoot, newSignerFp, "Test registration");

        assertNotNull(record);
        assertNotNull(record.getId());
        assertEquals(newVersion, record.getVersion());
        assertEquals(newVariant, record.getVariant());

        // Verify it can be retrieved
        assertTrue(verificationService.isKnownVersion(newVersion, newVariant));

        // Cleanup
        integrityRepository.delete("version = ?1", newVersion);
    }

    @Test
    @Order(8)
    @DisplayName("Should check if version is known")
    void testIsKnownVersion() {
        assertTrue(verificationService.isKnownVersion(TEST_VERSION, TEST_VARIANT));
        assertFalse(verificationService.isKnownVersion("unknown", "unknown"));
    }
}
