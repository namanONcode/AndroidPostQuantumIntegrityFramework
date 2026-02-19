package com.anchorpq.server.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.anchorpq.server.crypto.CryptoService;
import com.anchorpq.server.model.IntegrityPayload;
import com.anchorpq.server.model.IntegrityRecord;
import com.anchorpq.server.model.VerificationRequest;
import com.anchorpq.server.repository.IntegrityRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;

/** Integration tests for verification endpoint. */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VerificationResourceTest {

    @Inject CryptoService cryptoService;

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
        // Clean up and create test record
        integrityRepository.delete("version = ?1 and variant = ?2", TEST_VERSION, TEST_VARIANT);

        IntegrityRecord record =
                new IntegrityRecord(TEST_VERSION, TEST_VARIANT, TEST_MERKLE_ROOT, TEST_SIGNER_FP);
        integrityRepository.persist(record);
    }

    @AfterEach
    @Transactional
    void cleanup() {
        integrityRepository.delete("version = ?1", TEST_VERSION);
    }

    @Test
    @Order(1)
    @DisplayName("Should verify valid encrypted request and return APPROVED")
    void testVerifyValidRequest() {
        // Create a valid integrity payload
        IntegrityPayload payload =
                new IntegrityPayload(TEST_MERKLE_ROOT, TEST_VERSION, TEST_VARIANT, TEST_SIGNER_FP);

        // Encrypt the payload using the crypto service (simulating client)
        VerificationRequest request = cryptoService.createEncryptedRequest(payload);

        // Make the verification request
        given().contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/verify")
                .then()
                .statusCode(200)
                .body("status", equalTo("APPROVED"))
                .body("message", containsString("verified"))
                .body("timestamp", notNullValue());
    }

    @Test
    @Order(2)
    @DisplayName("Should reject invalid Merkle root")
    void testVerifyInvalidMerkleRoot() {
        IntegrityPayload payload =
                new IntegrityPayload(
                        "0000000000000000000000000000000000000000000000000000000000000000",
                        TEST_VERSION,
                        TEST_VARIANT,
                        TEST_SIGNER_FP);

        VerificationRequest request = cryptoService.createEncryptedRequest(payload);

        given().contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/verify")
                .then()
                .statusCode(200)
                .body("status", equalTo("REJECTED"))
                .body("errorCode", equalTo("ERR_MERKLE_MISMATCH"));
    }

    @Test
    @Order(3)
    @DisplayName("Should restrict for invalid signer")
    void testVerifyInvalidSigner() {
        IntegrityPayload payload =
                new IntegrityPayload(
                        TEST_MERKLE_ROOT,
                        TEST_VERSION,
                        TEST_VARIANT,
                        "0000000000000000000000000000000000000000000000000000000000000000");

        VerificationRequest request = cryptoService.createEncryptedRequest(payload);

        given().contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/verify")
                .then()
                .statusCode(200)
                .body("status", equalTo("RESTRICTED"));
    }

    @Test
    @Order(4)
    @DisplayName("Should reject unknown version")
    void testVerifyUnknownVersion() {
        IntegrityPayload payload =
                new IntegrityPayload(TEST_MERKLE_ROOT, "99.99.99", TEST_VARIANT, TEST_SIGNER_FP);

        VerificationRequest request = cryptoService.createEncryptedRequest(payload);

        given().contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/verify")
                .then()
                .statusCode(200)
                .body("status", equalTo("REJECTED"))
                .body("errorCode", equalTo("ERR_UNKNOWN_VERSION"));
    }

    @Test
    @Order(5)
    @DisplayName("Should return 400 for invalid encrypted payload")
    void testVerifyInvalidEncryption() {
        VerificationRequest request = new VerificationRequest();
        request.setEncapsulatedKey("invalidbase64!!!");
        request.setEncryptedPayload("alsoInvalid!!!");

        given().contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/verify")
                .then()
                .statusCode(400)
                .body("status", equalTo("REJECTED"));
    }

    @Test
    @Order(6)
    @DisplayName("Should return 400 for missing fields")
    void testVerifyMissingFields() {
        VerificationRequest request = new VerificationRequest();
        // Missing both required fields

        given().contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/verify")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(7)
    @DisplayName("Should include rate limit header in response")
    void testRateLimitHeader() {
        IntegrityPayload payload =
                new IntegrityPayload(TEST_MERKLE_ROOT, TEST_VERSION, TEST_VARIANT, TEST_SIGNER_FP);

        VerificationRequest request = cryptoService.createEncryptedRequest(payload);

        given().contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/verify")
                .then()
                .statusCode(200)
                .header("X-RateLimit-Remaining", notNullValue());
    }
}
