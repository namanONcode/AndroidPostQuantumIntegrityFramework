package com.anchorpq.server.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.anchorpq.server.model.IntegrityRecord;
import com.anchorpq.server.repository.IntegrityRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
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
 * Real-World Integration Test: Simulating Full APK Build Pipeline
 *
 * <p>This test simulates a realistic scenario where: 1. An APK is built with multiple DEX files 2.
 * The Gradle plugin computes the Merkle root during build 3. The CI/CD pipeline registers the
 * canonical root with the server 4. At runtime, the app verifies its integrity with the server
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RealWorldApkIntegrationTest {

    private static final String KEM_ALGORITHM = "Kyber";
    private static final String PROVIDER = "BCPQC";
    private static final String AES_GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_NONCE_LENGTH = 12;
    private static final String HKDF_INFO = "AnchorPQ-v1-IntegrityVerification";

    private static final String APP_VERSION = "4.2.0";
    private static final String APP_VARIANT = "release";

    private static Path tempApkDir;
    private static String computedMerkleRoot;
    private static String signerFingerprint;

    @Inject IntegrityRepository integrityRepository;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        if (Security.getProvider("BCPQC") == null) {
            Security.addProvider(new BouncyCastlePQCProvider());
        }
    }

    @BeforeAll
    static void setupApkSimulation() throws Exception {
        // Create a temporary directory to simulate APK contents
        tempApkDir = Files.createTempDirectory("test-apk");

        // Simulate creating DEX files with different content
        createSimulatedDexFile(
                tempApkDir.resolve("classes.dex"),
                "Main application code - Activity, Fragment, ViewModel classes");
        createSimulatedDexFile(
                tempApkDir.resolve("classes2.dex"),
                "Library code - Retrofit, OkHttp, Gson dependencies");
        createSimulatedDexFile(
                tempApkDir.resolve("classes3.dex"), "More library code - AndroidX components");
        createSimulatedResourceFile(
                tempApkDir.resolve("resources.arsc"),
                "Compiled resources - strings, layouts, drawables");
        createSimulatedFile(
                tempApkDir.resolve("AndroidManifest.xml"),
                "<?xml version=\"1.0\"?><manifest package=\"com.example.app\"/>");

        // Compute signer fingerprint (simulating the APK signing certificate)
        signerFingerprint = computeSignerFingerprint("CN=Test Developer, O=Test Org");

        // Compute Merkle root from all critical files
        computedMerkleRoot = computeMerkleRootFromApk(tempApkDir);

        System.out.println("=".repeat(60));
        System.out.println("APK Simulation Setup Complete");
        System.out.println("=".repeat(60));
        System.out.println("APK Directory: " + tempApkDir);
        System.out.println("Computed Merkle Root: " + computedMerkleRoot);
        System.out.println("Signer Fingerprint: " + signerFingerprint);
        System.out.println("=".repeat(60));
    }

    @AfterAll
    static void cleanup() throws Exception {
        // Clean up temp files
        if (tempApkDir != null) {
            Files.walk(tempApkDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(
                            path -> {
                                try {
                                    Files.delete(path);
                                } catch (IOException ignored) {
                                }
                            });
        }
    }

    @BeforeEach
    @Transactional
    void setupRecord() {
        // Clean up and register the canonical record (simulating CI/CD registration)
        integrityRepository.delete("version = ?1 and variant = ?2", APP_VERSION, APP_VARIANT);

        IntegrityRecord record =
                new IntegrityRecord(
                        APP_VERSION, APP_VARIANT, computedMerkleRoot, signerFingerprint);
        record.setDescription("Production release build from CI/CD pipeline");
        integrityRepository.persist(record);
    }

    @AfterEach
    @Transactional
    void cleanupRecord() {
        integrityRepository.delete("version = ?1", APP_VERSION);
    }

    // =========================================================================
    // TEST SCENARIOS
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("Scenario: Legitimate app verifies successfully after fresh install")
    void testLegitimateAppVerification() throws Exception {
        // Simulate the app runtime computing its own Merkle root
        String runtimeMerkleRoot = computeMerkleRootFromApk(tempApkDir);

        // Verify it matches what was computed at build time
        assertEquals(
                computedMerkleRoot,
                runtimeMerkleRoot,
                "Runtime Merkle root should match build-time computation");

        // Perform verification with server
        VerificationResult result =
                performVerification(runtimeMerkleRoot, APP_VERSION, APP_VARIANT, signerFingerprint);

        assertEquals("APPROVED", result.status);
        System.out.println("✅ Legitimate app verification: APPROVED");
    }

    @Test
    @Order(2)
    @DisplayName("Scenario: Tampered DEX file detected")
    void testTamperedDexDetection() throws Exception {
        // Create a copy of the APK with modified DEX
        Path tamperedDir = Files.createTempDirectory("tampered-apk");
        try {
            // Copy original files
            for (File file : tempApkDir.toFile().listFiles()) {
                Files.copy(file.toPath(), tamperedDir.resolve(file.getName()));
            }

            // Tamper with classes.dex (inject malicious code)
            createSimulatedDexFile(
                    tamperedDir.resolve("classes.dex"),
                    "Main application code - Activity, Fragment, ViewModel classes + MALICIOUS PAYLOAD INJECTED");

            // Compute Merkle root of tampered APK
            String tamperedMerkleRoot = computeMerkleRootFromApk(tamperedDir);

            // Verify it's different from original
            assertNotEquals(
                    computedMerkleRoot,
                    tamperedMerkleRoot,
                    "Tampered APK should have different Merkle root");

            // Server should reject
            VerificationResult result =
                    performVerification(
                            tamperedMerkleRoot, APP_VERSION, APP_VARIANT, signerFingerprint);

            assertEquals("REJECTED", result.status);
            assertEquals("ERR_MERKLE_MISMATCH", result.errorCode);
            System.out.println("✅ Tampered DEX detection: REJECTED (tampering detected!)");

        } finally {
            // Cleanup
            Files.walk(tamperedDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(
                            path -> {
                                try {
                                    Files.delete(path);
                                } catch (IOException ignored) {
                                }
                            });
        }
    }

    @Test
    @Order(3)
    @DisplayName("Scenario: Repackaged app with different signer")
    void testRepackagedAppDetection() throws Exception {
        // Same Merkle root but different signer (attacker repackaged the app)
        String attackerSignerFp = computeSignerFingerprint("CN=Attacker, O=Malicious Org");

        assertNotEquals(signerFingerprint, attackerSignerFp);

        VerificationResult result =
                performVerification(computedMerkleRoot, APP_VERSION, APP_VARIANT, attackerSignerFp);

        assertEquals("RESTRICTED", result.status);
        System.out.println("✅ Repackaged app detection: RESTRICTED (unknown signer)");
    }

    @Test
    @Order(4)
    @DisplayName("Scenario: Old version of app attempts to verify")
    void testOldVersionDetection() throws Exception {
        String oldVersion = "1.0.0"; // Not registered

        VerificationResult result =
                performVerification(computedMerkleRoot, oldVersion, APP_VARIANT, signerFingerprint);

        assertEquals("REJECTED", result.status);
        assertEquals("ERR_UNKNOWN_VERSION", result.errorCode);
        System.out.println("✅ Old version detection: REJECTED (unknown version)");
    }

    @Test
    @Order(5)
    @DisplayName("Scenario: Debug build attempting to verify against release record")
    void testVariantMismatch() throws Exception {
        String debugVariant = "debug"; // Not registered

        VerificationResult result =
                performVerification(
                        computedMerkleRoot, APP_VERSION, debugVariant, signerFingerprint);

        assertEquals("REJECTED", result.status);
        assertEquals("ERR_UNKNOWN_VERSION", result.errorCode);
        System.out.println("✅ Variant mismatch detection: REJECTED (wrong variant)");
    }

    @Test
    @Order(6)
    @DisplayName("Scenario: Concurrent verification requests from multiple app instances")
    void testConcurrentVerifications() throws Exception {
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        boolean[] results = new boolean[threadCount];
        Exception[] exceptions = new Exception[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] =
                    new Thread(
                            () -> {
                                try {
                                    VerificationResult result =
                                            performVerification(
                                                    computedMerkleRoot,
                                                    APP_VERSION,
                                                    APP_VARIANT,
                                                    signerFingerprint);
                                    results[index] = "APPROVED".equals(result.status);
                                } catch (Exception e) {
                                    exceptions[index] = e;
                                }
                            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for completion
        for (Thread thread : threads) {
            thread.join();
        }

        // Verify all succeeded
        for (int i = 0; i < threadCount; i++) {
            if (exceptions[i] != null) {
                fail("Thread " + i + " threw exception: " + exceptions[i].getMessage());
            }
            assertTrue(results[i], "Thread " + i + " should have received APPROVED");
        }

        System.out.println("✅ Concurrent verifications: All " + threadCount + " requests APPROVED");
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private static void createSimulatedDexFile(Path path, String content) throws IOException {
        // DEX file header magic: "dex\n035\0"
        byte[] dexMagic = {0x64, 0x65, 0x78, 0x0a, 0x30, 0x33, 0x35, 0x00};
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            fos.write(dexMagic);
            fos.write(contentBytes);
        }
    }

    private static void createSimulatedResourceFile(Path path, String content) throws IOException {
        // ARSC file header magic
        byte[] arscMagic = {0x02, 0x00, 0x0c, 0x00};
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            fos.write(arscMagic);
            fos.write(contentBytes);
        }
    }

    private static void createSimulatedFile(Path path, String content) throws IOException {
        Files.writeString(path, content);
    }

    private static String computeSignerFingerprint(String distinguishedName) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(distinguishedName.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private static String computeMerkleRootFromApk(Path apkDir) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        List<byte[]> leafHashes = new ArrayList<>();

        // Sort files for deterministic ordering
        File[] files = apkDir.toFile().listFiles();
        if (files != null) {
            Arrays.sort(files, Comparator.comparing(File::getName));

            for (File file : files) {
                if (file.isFile()) {
                    byte[] fileContent = Files.readAllBytes(file.toPath());
                    byte[] fileHash = digest.digest(fileContent);
                    leafHashes.add(fileHash);
                }
            }
        }

        return computeMerkleRoot(leafHashes);
    }

    private static String computeMerkleRoot(List<byte[]> leafHashes) throws Exception {
        if (leafHashes.isEmpty()) {
            throw new IllegalArgumentException("No leaf hashes provided");
        }

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        List<byte[]> currentLevel = new ArrayList<>(leafHashes);

        while (currentLevel.size() > 1) {
            List<byte[]> nextLevel = new ArrayList<>();

            for (int i = 0; i < currentLevel.size(); i += 2) {
                byte[] left = currentLevel.get(i);
                byte[] right = (i + 1 < currentLevel.size()) ? currentLevel.get(i + 1) : left;

                digest.reset();
                digest.update(left);
                digest.update(right);
                nextLevel.add(digest.digest());
            }

            currentLevel = nextLevel;
        }

        return bytesToHex(currentLevel.get(0));
    }

    private VerificationResult performVerification(
            String merkleRoot, String version, String variant, String signerFp) throws Exception {

        // Step 1: Fetch public key
        String publicKeyBase64 =
                given().when()
                        .get("/public-key")
                        .then()
                        .statusCode(200)
                        .extract()
                        .path("publicKey");

        byte[] serverPublicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);

        // Step 2: Encapsulate shared secret
        KeyFactory keyFactory = KeyFactory.getInstance(KEM_ALGORITHM, PROVIDER);
        PublicKey serverPublicKey =
                keyFactory.generatePublic(new X509EncodedKeySpec(serverPublicKeyBytes));

        KeyGenerator keyGenerator = KeyGenerator.getInstance(KEM_ALGORITHM, PROVIDER);
        keyGenerator.init(new KEMGenerateSpec(serverPublicKey, "AES"), new SecureRandom());
        SecretKeyWithEncapsulation secretKeyWithEncap =
                (SecretKeyWithEncapsulation) keyGenerator.generateKey();

        byte[] encapsulatedKey = secretKeyWithEncap.getEncapsulation();
        byte[] sharedSecret = secretKeyWithEncap.getEncoded();

        // Step 3: Derive AES key using HKDF-SHA3-256
        SecretKey aesKey = deriveAesKey(sharedSecret);

        // Step 4: Create and encrypt payload
        String payloadJson =
                String.format(
                        "{\"merkleRoot\":\"%s\",\"version\":\"%s\",\"variant\":\"%s\",\"signerFingerprint\":\"%s\"}",
                        merkleRoot, version, variant, signerFp);

        String encryptedPayload =
                encryptAesGcm(aesKey, payloadJson.getBytes(StandardCharsets.UTF_8));

        // Step 5: Send verification request
        String requestBody =
                String.format(
                        "{\"encapsulatedKey\":\"%s\",\"encryptedPayload\":\"%s\",\"timestamp\":%d}",
                        Base64.getEncoder().encodeToString(encapsulatedKey),
                        encryptedPayload,
                        System.currentTimeMillis());

        io.restassured.response.Response response =
                given().contentType(ContentType.JSON).body(requestBody).when().post("/verify");

        String status = response.path("status");
        String errorCode = response.path("errorCode");

        return new VerificationResult(status, errorCode);
    }

    private SecretKey deriveAesKey(byte[] sharedSecret) {
        SHA3Digest sha3Digest = new SHA3Digest(256);
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(sha3Digest);

        byte[] infoBytes = HKDF_INFO.getBytes(StandardCharsets.UTF_8);
        HKDFParameters params = HKDFParameters.skipExtractParameters(sharedSecret, infoBytes);
        hkdf.init(params);

        byte[] derivedKey = new byte[32];
        hkdf.generateBytes(derivedKey, 0, derivedKey.length);

        return new SecretKeySpec(derivedKey, "AES");
    }

    private String encryptAesGcm(SecretKey key, byte[] plaintext) throws Exception {
        byte[] nonce = new byte[GCM_NONCE_LENGTH];
        new SecureRandom().nextBytes(nonce);

        Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, nonce));

        byte[] ciphertext = cipher.doFinal(plaintext);

        ByteBuffer buffer = ByteBuffer.allocate(nonce.length + ciphertext.length);
        buffer.put(nonce);
        buffer.put(ciphertext);

        return Base64.getEncoder().encodeToString(buffer.array());
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private record VerificationResult(String status, String errorCode) {}
}
