package com.anchorpq.server.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.anchorpq.server.crypto.AesGcmService;
import com.anchorpq.server.crypto.MLKemService;
import com.anchorpq.server.model.IntegrityRecord;
import com.anchorpq.server.repository.IntegrityRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.jcajce.SecretKeyWithEncapsulation;
import org.bouncycastle.jcajce.spec.KEMGenerateSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.junit.jupiter.api.*;

/**
 * Full End-to-End Integration Test: Plugin Client → Server Verification
 *
 * <p>This test simulates the complete flow: 1. Client (plugin) computes Merkle root from files 2.
 * Client fetches server's ML-KEM public key 3. Client encapsulates shared secret and derives AES
 * key 4. Client encrypts integrity payload with AES-GCM 5. Client sends verification request to
 * server 6. Server decapsulates, decrypts, and verifies 7. Server returns verification decision
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PluginToServerIntegrationTest {

    private static final String KEM_ALGORITHM = "Kyber";
    private static final String PROVIDER = "BCPQC";
    private static final String AES_GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_NONCE_LENGTH = 12;
    private static final String HKDF_INFO = "AnchorPQ-v1-IntegrityVerification";

    // Test application data
    private static final String TEST_VERSION = "2.0.0";
    private static final String TEST_VARIANT = "release";
    private static final String TEST_SIGNER_FP =
            "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";

    // This Merkle root will be computed from simulated DEX files
    private static String computedMerkleRoot;

    @Inject MLKemService mlKemService;

    @Inject AesGcmService aesGcmService;

    @Inject IntegrityRepository integrityRepository;

    static {
        // Register Bouncy Castle providers (simulating plugin-side setup)
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        if (Security.getProvider("BCPQC") == null) {
            Security.addProvider(new BouncyCastlePQCProvider());
        }
    }

    @BeforeAll
    static void computeMerkleRoot() {
        // Simulate computing Merkle root from DEX files (like the plugin does)
        List<byte[]> fileHashes = simulateComputeFileHashes();
        computedMerkleRoot = computeMerkleRootFromHashes(fileHashes);
        System.out.println("Computed Merkle Root: " + computedMerkleRoot);
    }

    @BeforeEach
    @Transactional
    void setup() {
        // Clean up any existing test data
        integrityRepository.delete("version = ?1 and variant = ?2", TEST_VERSION, TEST_VARIANT);

        // Register canonical integrity record (simulating CI/CD pipeline registration)
        IntegrityRecord record =
                new IntegrityRecord(TEST_VERSION, TEST_VARIANT, computedMerkleRoot, TEST_SIGNER_FP);
        record.setDescription("Integration test record");
        integrityRepository.persist(record);
    }

    @AfterEach
    @Transactional
    void cleanup() {
        integrityRepository.delete("version = ?1", TEST_VERSION);
    }

    // =========================================================================
    // INTEGRATION TEST: Complete Plugin to Server Flow
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName(
            "Full E2E: Plugin computes Merkle root, encrypts with ML-KEM, server verifies - APPROVED")
    void testFullEndToEndFlow_Approved() throws Exception {
        // =================================================================
        // STEP 1: Fetch server's ML-KEM public key (GET /public-key)
        // =================================================================
        String publicKeyBase64 =
                given().when()
                        .get("/public-key")
                        .then()
                        .statusCode(200)
                        .body("publicKey", notNullValue())
                        .body("parameterSet", notNullValue())
                        .body("algorithm", equalTo("ML-KEM"))
                        .extract()
                        .path("publicKey");

        byte[] serverPublicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
        System.out.println(
                "Fetched server public key, size: " + serverPublicKeyBytes.length + " bytes");

        // =================================================================
        // STEP 2: Plugin-side - Parse public key and encapsulate shared secret
        // =================================================================
        KeyFactory keyFactory = KeyFactory.getInstance(KEM_ALGORITHM, PROVIDER);
        PublicKey serverPublicKey =
                keyFactory.generatePublic(new X509EncodedKeySpec(serverPublicKeyBytes));

        // Encapsulate shared secret (client-side KEM operation)
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KEM_ALGORITHM, PROVIDER);
        keyGenerator.init(new KEMGenerateSpec(serverPublicKey, "AES"), new SecureRandom());
        SecretKeyWithEncapsulation secretKeyWithEncap =
                (SecretKeyWithEncapsulation) keyGenerator.generateKey();

        byte[] encapsulatedKey = secretKeyWithEncap.getEncapsulation();
        byte[] sharedSecret = secretKeyWithEncap.getEncoded();

        System.out.println(
                "Encapsulated shared secret, ciphertext size: "
                        + encapsulatedKey.length
                        + " bytes");

        // =================================================================
        // STEP 3: Plugin-side - Derive AES key using HKDF-SHA3-256
        // =================================================================
        SecretKey aesKey = deriveAesKeyHkdfSha3(sharedSecret);
        System.out.println("Derived AES-256 key using HKDF-SHA3-256");

        // =================================================================
        // STEP 4: Plugin-side - Create and encrypt integrity payload
        // =================================================================
        String payloadJson =
                String.format(
                        "{\"merkleRoot\":\"%s\",\"version\":\"%s\",\"variant\":\"%s\",\"signerFingerprint\":\"%s\"}",
                        computedMerkleRoot, TEST_VERSION, TEST_VARIANT, TEST_SIGNER_FP);
        System.out.println("Integrity payload: " + payloadJson);

        String encryptedPayload =
                encryptWithAesGcm(aesKey, payloadJson.getBytes(StandardCharsets.UTF_8));
        System.out.println(
                "Encrypted payload (base64), size: " + encryptedPayload.length() + " chars");

        // =================================================================
        // STEP 5: Send verification request to server (POST /verify)
        // =================================================================
        String requestBody =
                String.format(
                        "{\"encapsulatedKey\":\"%s\",\"encryptedPayload\":\"%s\",\"timestamp\":%d}",
                        Base64.getEncoder().encodeToString(encapsulatedKey),
                        encryptedPayload,
                        System.currentTimeMillis());

        given().contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/verify")
                .then()
                .statusCode(200)
                .body("status", equalTo("APPROVED"))
                .body("message", containsString("verified"))
                .body("timestamp", notNullValue());

        System.out.println("✅ Full E2E test PASSED - Server returned APPROVED");
    }

    @Test
    @Order(2)
    @DisplayName("Full E2E: Tampered Merkle root - Server returns REJECTED")
    void testFullEndToEndFlow_TamperedMerkleRoot() throws Exception {
        // Fetch server's public key
        String publicKeyBase64 =
                given().when()
                        .get("/public-key")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("publicKey");

        byte[] serverPublicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);

        // Encapsulate shared secret
        KeyFactory keyFactory = KeyFactory.getInstance(KEM_ALGORITHM, PROVIDER);
        PublicKey serverPublicKey =
                keyFactory.generatePublic(new X509EncodedKeySpec(serverPublicKeyBytes));

        KeyGenerator keyGenerator = KeyGenerator.getInstance(KEM_ALGORITHM, PROVIDER);
        keyGenerator.init(new KEMGenerateSpec(serverPublicKey, "AES"), new SecureRandom());
        SecretKeyWithEncapsulation secretKeyWithEncap =
                (SecretKeyWithEncapsulation) keyGenerator.generateKey();

        byte[] encapsulatedKey = secretKeyWithEncap.getEncapsulation();
        byte[] sharedSecret = secretKeyWithEncap.getEncoded();

        // Derive AES key
        SecretKey aesKey = deriveAesKeyHkdfSha3(sharedSecret);

        // Create payload with TAMPERED Merkle root
        String tamperedMerkleRoot =
                "0000000000000000000000000000000000000000000000000000000000000000";
        String payloadJson =
                String.format(
                        "{\"merkleRoot\":\"%s\",\"version\":\"%s\",\"variant\":\"%s\",\"signerFingerprint\":\"%s\"}",
                        tamperedMerkleRoot, TEST_VERSION, TEST_VARIANT, TEST_SIGNER_FP);

        String encryptedPayload =
                encryptWithAesGcm(aesKey, payloadJson.getBytes(StandardCharsets.UTF_8));

        String requestBody =
                String.format(
                        "{\"encapsulatedKey\":\"%s\",\"encryptedPayload\":\"%s\",\"timestamp\":%d}",
                        Base64.getEncoder().encodeToString(encapsulatedKey),
                        encryptedPayload,
                        System.currentTimeMillis());

        given().contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/verify")
                .then()
                .statusCode(200)
                .body("status", equalTo("REJECTED"))
                .body("errorCode", equalTo("ERR_MERKLE_MISMATCH"));

        System.out.println("✅ Tampered Merkle root test PASSED - Server returned REJECTED");
    }

    @Test
    @Order(3)
    @DisplayName("Full E2E: Invalid signer fingerprint - Server returns RESTRICTED")
    void testFullEndToEndFlow_InvalidSigner() throws Exception {
        // Fetch server's public key
        String publicKeyBase64 =
                given().when()
                        .get("/public-key")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("publicKey");

        byte[] serverPublicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);

        // Encapsulate shared secret
        KeyFactory keyFactory = KeyFactory.getInstance(KEM_ALGORITHM, PROVIDER);
        PublicKey serverPublicKey =
                keyFactory.generatePublic(new X509EncodedKeySpec(serverPublicKeyBytes));

        KeyGenerator keyGenerator = KeyGenerator.getInstance(KEM_ALGORITHM, PROVIDER);
        keyGenerator.init(new KEMGenerateSpec(serverPublicKey, "AES"), new SecureRandom());
        SecretKeyWithEncapsulation secretKeyWithEncap =
                (SecretKeyWithEncapsulation) keyGenerator.generateKey();

        byte[] encapsulatedKey = secretKeyWithEncap.getEncapsulation();
        byte[] sharedSecret = secretKeyWithEncap.getEncoded();

        // Derive AES key
        SecretKey aesKey = deriveAesKeyHkdfSha3(sharedSecret);

        // Create payload with WRONG signer fingerprint
        String wrongSignerFp = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
        String payloadJson =
                String.format(
                        "{\"merkleRoot\":\"%s\",\"version\":\"%s\",\"variant\":\"%s\",\"signerFingerprint\":\"%s\"}",
                        computedMerkleRoot, TEST_VERSION, TEST_VARIANT, wrongSignerFp);

        String encryptedPayload =
                encryptWithAesGcm(aesKey, payloadJson.getBytes(StandardCharsets.UTF_8));

        String requestBody =
                String.format(
                        "{\"encapsulatedKey\":\"%s\",\"encryptedPayload\":\"%s\",\"timestamp\":%d}",
                        Base64.getEncoder().encodeToString(encapsulatedKey),
                        encryptedPayload,
                        System.currentTimeMillis());

        given().contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/verify")
                .then()
                .statusCode(200)
                .body("status", equalTo("RESTRICTED"));

        System.out.println("✅ Invalid signer test PASSED - Server returned RESTRICTED");
    }

    @Test
    @Order(4)
    @DisplayName("Full E2E: Unknown version - Server returns REJECTED")
    void testFullEndToEndFlow_UnknownVersion() throws Exception {
        // Fetch server's public key
        String publicKeyBase64 =
                given().when()
                        .get("/public-key")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("publicKey");

        byte[] serverPublicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);

        // Encapsulate shared secret
        KeyFactory keyFactory = KeyFactory.getInstance(KEM_ALGORITHM, PROVIDER);
        PublicKey serverPublicKey =
                keyFactory.generatePublic(new X509EncodedKeySpec(serverPublicKeyBytes));

        KeyGenerator keyGenerator = KeyGenerator.getInstance(KEM_ALGORITHM, PROVIDER);
        keyGenerator.init(new KEMGenerateSpec(serverPublicKey, "AES"), new SecureRandom());
        SecretKeyWithEncapsulation secretKeyWithEncap =
                (SecretKeyWithEncapsulation) keyGenerator.generateKey();

        byte[] encapsulatedKey = secretKeyWithEncap.getEncapsulation();
        byte[] sharedSecret = secretKeyWithEncap.getEncoded();

        // Derive AES key
        SecretKey aesKey = deriveAesKeyHkdfSha3(sharedSecret);

        // Create payload with UNKNOWN version
        String unknownVersion = "99.99.99";
        String payloadJson =
                String.format(
                        "{\"merkleRoot\":\"%s\",\"version\":\"%s\",\"variant\":\"%s\",\"signerFingerprint\":\"%s\"}",
                        computedMerkleRoot, unknownVersion, TEST_VARIANT, TEST_SIGNER_FP);

        String encryptedPayload =
                encryptWithAesGcm(aesKey, payloadJson.getBytes(StandardCharsets.UTF_8));

        String requestBody =
                String.format(
                        "{\"encapsulatedKey\":\"%s\",\"encryptedPayload\":\"%s\",\"timestamp\":%d}",
                        Base64.getEncoder().encodeToString(encapsulatedKey),
                        encryptedPayload,
                        System.currentTimeMillis());

        given().contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/verify")
                .then()
                .statusCode(200)
                .body("status", equalTo("REJECTED"))
                .body("errorCode", equalTo("ERR_UNKNOWN_VERSION"));

        System.out.println("✅ Unknown version test PASSED - Server returned REJECTED");
    }

    @Test
    @Order(5)
    @DisplayName("Full E2E: Multiple consecutive verifications work correctly")
    void testMultipleVerifications() throws Exception {
        for (int i = 0; i < 5; i++) {
            // Each iteration uses fresh encapsulation
            String publicKeyBase64 =
                    given().when()
                            .get("/public-key")
                            .then()
                            .statusCode(200)
                            .extract()
                            .path("publicKey");

            byte[] serverPublicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);

            KeyFactory keyFactory = KeyFactory.getInstance(KEM_ALGORITHM, PROVIDER);
            PublicKey serverPublicKey =
                    keyFactory.generatePublic(new X509EncodedKeySpec(serverPublicKeyBytes));

            KeyGenerator keyGenerator = KeyGenerator.getInstance(KEM_ALGORITHM, PROVIDER);
            keyGenerator.init(new KEMGenerateSpec(serverPublicKey, "AES"), new SecureRandom());
            SecretKeyWithEncapsulation secretKeyWithEncap =
                    (SecretKeyWithEncapsulation) keyGenerator.generateKey();

            byte[] encapsulatedKey = secretKeyWithEncap.getEncapsulation();
            byte[] sharedSecret = secretKeyWithEncap.getEncoded();

            SecretKey aesKey = deriveAesKeyHkdfSha3(sharedSecret);

            String payloadJson =
                    String.format(
                            "{\"merkleRoot\":\"%s\",\"version\":\"%s\",\"variant\":\"%s\",\"signerFingerprint\":\"%s\"}",
                            computedMerkleRoot, TEST_VERSION, TEST_VARIANT, TEST_SIGNER_FP);

            String encryptedPayload =
                    encryptWithAesGcm(aesKey, payloadJson.getBytes(StandardCharsets.UTF_8));

            String requestBody =
                    String.format(
                            "{\"encapsulatedKey\":\"%s\",\"encryptedPayload\":\"%s\",\"timestamp\":%d}",
                            Base64.getEncoder().encodeToString(encapsulatedKey),
                            encryptedPayload,
                            System.currentTimeMillis());

            given().contentType(ContentType.JSON)
                    .body(requestBody)
                    .when()
                    .post("/verify")
                    .then()
                    .statusCode(200)
                    .body("status", equalTo("APPROVED"));
        }

        System.out.println(
                "✅ Multiple verifications test PASSED - All 5 iterations returned APPROVED");
    }

    @Test
    @Order(6)
    @DisplayName("Admin API: Register and verify new integrity record")
    void testAdminApiIntegration() throws Exception {
        String newVersion = "3.0.0";
        String newVariant = "debug";
        String newMerkleRoot = "1111111111111111111111111111111111111111111111111111111111111111";
        String newSignerFp = "2222222222222222222222222222222222222222222222222222222222222222";

        // Step 1: Register new record via Admin API
        String adminRequest =
                String.format(
                        "{\"version\":\"%s\",\"variant\":\"%s\",\"merkleRoot\":\"%s\",\"signerFingerprint\":\"%s\",\"description\":\"Test record\"}",
                        newVersion, newVariant, newMerkleRoot, newSignerFp);

        given().contentType(ContentType.JSON)
                .body(adminRequest)
                .when()
                .post("/admin/records")
                .then()
                .statusCode(201)
                .body("version", equalTo(newVersion))
                .body("variant", equalTo(newVariant));

        // Step 2: Verify the new record works
        String publicKeyBase64 =
                given().when()
                        .get("/public-key")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("publicKey");

        byte[] serverPublicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);

        KeyFactory keyFactory = KeyFactory.getInstance(KEM_ALGORITHM, PROVIDER);
        PublicKey serverPublicKey =
                keyFactory.generatePublic(new X509EncodedKeySpec(serverPublicKeyBytes));

        KeyGenerator keyGenerator = KeyGenerator.getInstance(KEM_ALGORITHM, PROVIDER);
        keyGenerator.init(new KEMGenerateSpec(serverPublicKey, "AES"), new SecureRandom());
        SecretKeyWithEncapsulation secretKeyWithEncap =
                (SecretKeyWithEncapsulation) keyGenerator.generateKey();

        byte[] encapsulatedKey = secretKeyWithEncap.getEncapsulation();
        byte[] sharedSecret = secretKeyWithEncap.getEncoded();

        SecretKey aesKey = deriveAesKeyHkdfSha3(sharedSecret);

        String payloadJson =
                String.format(
                        "{\"merkleRoot\":\"%s\",\"version\":\"%s\",\"variant\":\"%s\",\"signerFingerprint\":\"%s\"}",
                        newMerkleRoot, newVersion, newVariant, newSignerFp);

        String encryptedPayload =
                encryptWithAesGcm(aesKey, payloadJson.getBytes(StandardCharsets.UTF_8));

        String requestBody =
                String.format(
                        "{\"encapsulatedKey\":\"%s\",\"encryptedPayload\":\"%s\",\"timestamp\":%d}",
                        Base64.getEncoder().encodeToString(encapsulatedKey),
                        encryptedPayload,
                        System.currentTimeMillis());

        given().contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/verify")
                .then()
                .statusCode(200)
                .body("status", equalTo("APPROVED"));

        // Cleanup
        given().when()
                .delete("/admin/records/" + newVersion + "/" + newVariant)
                .then()
                .statusCode(204);

        System.out.println("✅ Admin API integration test PASSED");
    }

    // =========================================================================
    // HELPER METHODS - Simulating Plugin-Side Operations
    // =========================================================================

    /** Simulates computing file hashes like the plugin does for DEX files. */
    private static List<byte[]> simulateComputeFileHashes() {
        List<byte[]> hashes = new ArrayList<>();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Simulate hashing multiple DEX files
            hashes.add(
                    digest.digest(
                            "classes.dex content simulation".getBytes(StandardCharsets.UTF_8)));
            hashes.add(
                    digest.digest(
                            "classes2.dex content simulation".getBytes(StandardCharsets.UTF_8)));
            hashes.add(
                    digest.digest(
                            "classes3.dex content simulation".getBytes(StandardCharsets.UTF_8)));
            hashes.add(
                    digest.digest(
                            "resources.arsc content simulation".getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute file hashes", e);
        }
        return hashes;
    }

    /** Computes Merkle root from leaf hashes (like the plugin's MerkleTree class). */
    private static String computeMerkleRootFromHashes(List<byte[]> leafHashes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            List<byte[]> currentLevel = new ArrayList<>(leafHashes);

            while (currentLevel.size() > 1) {
                List<byte[]> nextLevel = new ArrayList<>();

                for (int i = 0; i < currentLevel.size(); i += 2) {
                    byte[] left = currentLevel.get(i);
                    byte[] right = (i + 1 < currentLevel.size()) ? currentLevel.get(i + 1) : left;

                    // Hash concatenation of left + right
                    digest.reset();
                    digest.update(left);
                    digest.update(right);
                    nextLevel.add(digest.digest());
                }

                currentLevel = nextLevel;
            }

            return bytesToHex(currentLevel.get(0));
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute Merkle root", e);
        }
    }

    /** Derives AES-256 key using HKDF-SHA3-256 (matching server's key derivation). */
    private SecretKey deriveAesKeyHkdfSha3(byte[] sharedSecret) {
        SHA3Digest sha3Digest = new SHA3Digest(256);
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(sha3Digest);

        byte[] infoBytes = HKDF_INFO.getBytes(StandardCharsets.UTF_8);
        HKDFParameters params = HKDFParameters.skipExtractParameters(sharedSecret, infoBytes);

        hkdf.init(params);

        byte[] derivedKey = new byte[32]; // 256 bits
        hkdf.generateBytes(derivedKey, 0, derivedKey.length);

        return new SecretKeySpec(derivedKey, "AES");
    }

    /**
     * Encrypts data using AES-256-GCM (matching server's expected format). Format: Base64(IV ||
     * Ciphertext || AuthTag)
     */
    private String encryptWithAesGcm(SecretKey key, byte[] plaintext) throws Exception {
        byte[] nonce = new byte[GCM_NONCE_LENGTH];
        new SecureRandom().nextBytes(nonce);

        Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, nonce));

        byte[] ciphertext = cipher.doFinal(plaintext);

        // Format: IV || Ciphertext (which includes auth tag in GCM)
        ByteBuffer buffer = ByteBuffer.allocate(nonce.length + ciphertext.length);
        buffer.put(nonce);
        buffer.put(ciphertext);

        return Base64.getEncoder().encodeToString(buffer.array());
    }

    /** Converts bytes to hex string. */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
