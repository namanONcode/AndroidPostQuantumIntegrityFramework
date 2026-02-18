package com.anchorpq.runtime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runtime helper for reporting application integrity to a backend server.
 *
 * <p>This class: 1. Reads the MERKLE_ROOT from BuildConfig or IntegrityConfig 2. Constructs an
 * integrity report payload 3. Encrypts the payload using ML-KEM + AES-GCM 4. Sends the encrypted
 * payload via IntegrityTransport
 *
 * <p>Usage in Android app:
 *
 * <pre>
 * // Initialize with server's public key
 * IntegrityReporter reporter = new IntegrityReporter(serverPublicKeyBytes);
 *
 * // Set transport implementation
 * reporter.setTransport(new HttpIntegrityTransport("https://api.example.com/integrity"));
 *
 * // Report integrity
 * IntegrityReporter.Result result = reporter.reportIntegrity(
 *     BuildConfig.MERKLE_ROOT,
 *     BuildConfig.VERSION_NAME,
 *     BuildConfig.BUILD_TYPE
 * );
 *
 * if (result.isSuccess()) {
 *     // Integrity reported successfully
 * }
 * </pre>
 */
public class IntegrityReporter {

  private final MLKemHelper kemHelper;
  private final PublicKey serverPublicKey;
  private IntegrityTransport transport;
  private final Gson gson;

  /**
   * Creates a new IntegrityReporter with the server's public key.
   *
   * @param serverPublicKeyBytes the server's ML-KEM public key (encoded)
   * @throws GeneralSecurityException if key decoding fails
   */
  public IntegrityReporter(byte[] serverPublicKeyBytes) throws GeneralSecurityException {
    this.kemHelper = new MLKemHelper();

    // Decode server public key
    KeyFactory keyFactory = KeyFactory.getInstance("Kyber", "BCPQC");
    this.serverPublicKey = keyFactory.generatePublic(new X509EncodedKeySpec(serverPublicKeyBytes));

    this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
  }

  /**
   * Creates a new IntegrityReporter with the server's public key.
   *
   * @param serverPublicKey the server's ML-KEM public key
   */
  public IntegrityReporter(PublicKey serverPublicKey) {
    this.kemHelper = new MLKemHelper();
    this.serverPublicKey = serverPublicKey;
    this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
  }

  /**
   * Sets the transport implementation.
   *
   * @param transport the transport to use
   */
  public void setTransport(IntegrityTransport transport) {
    this.transport = transport;
  }

  /**
   * Reports application integrity to the server.
   *
   * @param merkleRoot the computed Merkle root
   * @param version the application version
   * @param variant the build variant
   * @return the result of the report
   */
  public Result reportIntegrity(String merkleRoot, String version, String variant) {
    return reportIntegrity(merkleRoot, version, variant, null);
  }

  /**
   * Reports application integrity to the server with optional signer fingerprint.
   *
   * @param merkleRoot the computed Merkle root
   * @param version the application version
   * @param variant the build variant
   * @param signerFingerprint optional signing certificate fingerprint
   * @return the result of the report
   */
  public Result reportIntegrity(
      String merkleRoot, String version, String variant, String signerFingerprint) {
    if (transport == null) {
      return Result.failure("No transport configured");
    }

    if (!transport.isAvailable()) {
      return Result.failure("Transport not available");
    }

    try {
      Map<String, Object> report = buildReport(merkleRoot, version, variant, signerFingerprint);

      String json = gson.toJson(report);
      byte[] plaintext = json.getBytes(StandardCharsets.UTF_8);

      MLKemHelper.HybridEncryptedPayload payload =
          kemHelper.hybridEncrypt(plaintext, serverPublicKey);

      byte[] response = transport.sendAndReceive(payload.toBytes());

      return Result.success(response);

    } catch (GeneralSecurityException e) {
      return Result.failure("Encryption failed: " + e.getMessage());
    } catch (IntegrityTransport.IntegrityTransportException e) {
      return Result.failure("Transport failed: " + e.getMessage());
    }
  }

  /**
   * Reports integrity asynchronously.
   *
   * @param merkleRoot the computed Merkle root
   * @param version the application version
   * @param variant the build variant
   * @param callback callback for result
   */
  public void reportIntegrityAsync(
      String merkleRoot, String version, String variant, ResultCallback callback) {
    new Thread(
            () -> {
              Result result = reportIntegrity(merkleRoot, version, variant);
              callback.onResult(result);
            })
        .start();
  }

  /** Builds the integrity report payload. */
  private Map<String, Object> buildReport(
      String merkleRoot, String version, String variant, String signerFingerprint) {
    Map<String, Object> report = new LinkedHashMap<>();

    report.put("merkleRoot", merkleRoot);
    report.put("version", version);
    report.put("variant", variant);
    report.put("timestamp", Instant.now().toString());
    report.put("nonce", generateNonce());

    if (signerFingerprint != null && !signerFingerprint.isEmpty()) {
      report.put("signerFingerprint", signerFingerprint);
    }

    Map<String, String> deviceInfo = new LinkedHashMap<>();
    deviceInfo.put("sdk", System.getProperty("java.version", "unknown"));
    deviceInfo.put("runtime", System.getProperty("java.runtime.name", "unknown"));
    report.put("device", deviceInfo);

    return report;
  }

  /** Generates a random nonce for replay protection. */
  private String generateNonce() {
    byte[] nonce = new byte[16];
    new java.security.SecureRandom().nextBytes(nonce);
    return Base64.getEncoder().encodeToString(nonce);
  }

  /**
   * Gets the encrypted payload without sending. Useful for testing or custom transport
   * implementations.
   *
   * @param merkleRoot the computed Merkle root
   * @param version the application version
   * @param variant the build variant
   * @return the encrypted payload
   * @throws GeneralSecurityException if encryption fails
   */
  public byte[] getEncryptedPayload(String merkleRoot, String version, String variant)
      throws GeneralSecurityException {
    Map<String, Object> report = buildReport(merkleRoot, version, variant, null);
    String json = gson.toJson(report);
    byte[] plaintext = json.getBytes(StandardCharsets.UTF_8);

    MLKemHelper.HybridEncryptedPayload payload =
        kemHelper.hybridEncrypt(plaintext, serverPublicKey);

    return payload.toBytes();
  }

  /** Result of an integrity report operation. */
  public static class Result {
    private final boolean success;
    private final String errorMessage;
    private final byte[] serverResponse;

    private Result(boolean success, String errorMessage, byte[] serverResponse) {
      this.success = success;
      this.errorMessage = errorMessage;
      this.serverResponse = serverResponse;
    }

    public static Result success(byte[] response) {
      return new Result(true, null, response);
    }

    public static Result failure(String errorMessage) {
      return new Result(false, errorMessage, null);
    }

    public boolean isSuccess() {
      return success;
    }

    public String getErrorMessage() {
      return errorMessage;
    }

    public byte[] getServerResponse() {
      return serverResponse != null ? serverResponse.clone() : null;
    }

    public String getServerResponseString() {
      return serverResponse != null ? new String(serverResponse, StandardCharsets.UTF_8) : null;
    }
  }

  /** Callback for async operations. */
  public interface ResultCallback {
    void onResult(Result result);
  }
}
