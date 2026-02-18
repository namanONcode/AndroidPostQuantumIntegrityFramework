package com.anchorpq.runtime;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.Security;
import javax.crypto.SecretKey;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for MLKemHelper. */
class MLKemHelperTest {

  @BeforeAll
  static void setUp() {
    if (Security.getProvider("BCPQC") == null) {
      Security.addProvider(new BouncyCastlePQCProvider());
    }
  }

  @Test
  @DisplayName("Should generate key pair")
  void testKeyPairGeneration() throws GeneralSecurityException {
    MLKemHelper helper = new MLKemHelper();
    KeyPair keyPair = helper.generateKeyPair();

    assertNotNull(keyPair);
    assertNotNull(keyPair.getPublic());
    assertNotNull(keyPair.getPrivate());
  }

  @Test
  @DisplayName("Should encapsulate and decapsulate successfully")
  void testEncapsulationDecapsulation() throws GeneralSecurityException {
    MLKemHelper helper = new MLKemHelper();

    // Generate key pair (recipient side)
    KeyPair keyPair = helper.generateKeyPair();

    // Encapsulate (sender side)
    MLKemHelper.EncapsulationResult encapResult = helper.encapsulate(keyPair.getPublic());
    assertNotNull(encapResult.getCiphertext());
    assertNotNull(encapResult.getSharedSecret());

    // Decapsulate (recipient side)
    SecretKey decapsulatedSecret =
        helper.decapsulate(encapResult.getCiphertext(), keyPair.getPrivate());

    // Shared secrets should be equal
    assertArrayEquals(
        encapResult.getSharedSecret().getEncoded(),
        decapsulatedSecret.getEncoded(),
        "Shared secrets should match");
  }

  @Test
  @DisplayName("Should encrypt and decrypt data")
  void testEncryptDecrypt() throws GeneralSecurityException {
    MLKemHelper helper = new MLKemHelper();

    // Generate key pair
    KeyPair keyPair = helper.generateKeyPair();

    // Encapsulate to get shared secret
    MLKemHelper.EncapsulationResult encapResult = helper.encapsulate(keyPair.getPublic());
    SecretKey sharedSecret = encapResult.getSharedSecret();

    // Test data
    String plaintext = "This is a test message for encryption";
    byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);

    // Encrypt
    byte[] encrypted = helper.encrypt(plaintextBytes, sharedSecret);
    assertNotNull(encrypted);
    assertNotEquals(plaintextBytes.length, encrypted.length); // Should be longer due to nonce + tag

    // Decrypt
    byte[] decrypted = helper.decrypt(encrypted, sharedSecret);
    assertArrayEquals(plaintextBytes, decrypted);
    assertEquals(plaintext, new String(decrypted, StandardCharsets.UTF_8));
  }

  @Test
  @DisplayName("Should perform hybrid encryption")
  void testHybridEncryption() throws GeneralSecurityException {
    MLKemHelper helper = new MLKemHelper();

    // Generate recipient's key pair
    KeyPair recipientKeys = helper.generateKeyPair();

    // Sender encrypts
    String message = "{\"merkleRoot\":\"abc123\",\"version\":\"1.0.0\"}";
    byte[] plaintext = message.getBytes(StandardCharsets.UTF_8);

    MLKemHelper.HybridEncryptedPayload payload =
        helper.hybridEncrypt(plaintext, recipientKeys.getPublic());

    assertNotNull(payload);
    assertNotNull(payload.getKemCiphertext());
    assertNotNull(payload.getEncryptedData());

    // Recipient decrypts
    byte[] decrypted = helper.hybridDecrypt(payload, recipientKeys.getPrivate());
    assertArrayEquals(plaintext, decrypted);
    assertEquals(message, new String(decrypted, StandardCharsets.UTF_8));
  }

  @Test
  @DisplayName("Payload serialization should round-trip")
  void testPayloadSerialization() throws GeneralSecurityException {
    MLKemHelper helper = new MLKemHelper();
    KeyPair keyPair = helper.generateKeyPair();

    String message = "Test message for serialization";
    MLKemHelper.HybridEncryptedPayload original =
        helper.hybridEncrypt(message.getBytes(StandardCharsets.UTF_8), keyPair.getPublic());

    // Serialize and deserialize
    byte[] serialized = original.toBytes();
    MLKemHelper.HybridEncryptedPayload restored =
        MLKemHelper.HybridEncryptedPayload.fromBytes(serialized);

    assertArrayEquals(original.getKemCiphertext(), restored.getKemCiphertext());
    assertArrayEquals(original.getEncryptedData(), restored.getEncryptedData());

    // Verify decryption still works
    byte[] decrypted = helper.hybridDecrypt(restored, keyPair.getPrivate());
    assertEquals(message, new String(decrypted, StandardCharsets.UTF_8));
  }

  @Test
  @DisplayName("Base64 serialization should work")
  void testBase64Serialization() throws GeneralSecurityException {
    MLKemHelper helper = new MLKemHelper();
    KeyPair keyPair = helper.generateKeyPair();

    String message = "Test message";
    MLKemHelper.HybridEncryptedPayload original =
        helper.hybridEncrypt(message.getBytes(StandardCharsets.UTF_8), keyPair.getPublic());

    // Convert to/from Base64
    String base64 = original.toBase64();
    assertNotNull(base64);
    assertFalse(base64.isEmpty());

    MLKemHelper.HybridEncryptedPayload restored =
        MLKemHelper.HybridEncryptedPayload.fromBase64(base64);

    // Verify
    byte[] decrypted = helper.hybridDecrypt(restored, keyPair.getPrivate());
    assertEquals(message, new String(decrypted, StandardCharsets.UTF_8));
  }

  @Test
  @DisplayName("Different key pairs should not decrypt each other's messages")
  void testDifferentKeyPairs() throws GeneralSecurityException {
    MLKemHelper helper = new MLKemHelper();

    KeyPair keyPair1 = helper.generateKeyPair();
    KeyPair keyPair2 = helper.generateKeyPair();

    String message = "Secret message";
    MLKemHelper.HybridEncryptedPayload payload =
        helper.hybridEncrypt(message.getBytes(StandardCharsets.UTF_8), keyPair1.getPublic());

    // Should throw or return garbage when decrypting with wrong key
    assertThrows(
        Exception.class,
        () -> {
          helper.hybridDecrypt(payload, keyPair2.getPrivate());
        });
  }

  @Test
  @DisplayName("Should handle large data")
  void testLargeData() throws GeneralSecurityException {
    MLKemHelper helper = new MLKemHelper();
    KeyPair keyPair = helper.generateKeyPair();

    // Generate large data (1MB)
    byte[] largeData = new byte[1024 * 1024];
    new java.security.SecureRandom().nextBytes(largeData);

    MLKemHelper.HybridEncryptedPayload payload =
        helper.hybridEncrypt(largeData, keyPair.getPublic());

    byte[] decrypted = helper.hybridDecrypt(payload, keyPair.getPrivate());
    assertArrayEquals(largeData, decrypted);
  }

  @Test
  @DisplayName("Encapsulation should be different each time (randomized)")
  void testRandomizedEncapsulation() throws GeneralSecurityException {
    MLKemHelper helper = new MLKemHelper();
    KeyPair keyPair = helper.generateKeyPair();

    MLKemHelper.EncapsulationResult result1 = helper.encapsulate(keyPair.getPublic());
    MLKemHelper.EncapsulationResult result2 = helper.encapsulate(keyPair.getPublic());

    // Ciphertexts should be different (due to randomization)
    assertFalse(java.util.Arrays.equals(result1.getCiphertext(), result2.getCiphertext()));

    // But both should decapsulate to working keys
    SecretKey secret1 = helper.decapsulate(result1.getCiphertext(), keyPair.getPrivate());
    SecretKey secret2 = helper.decapsulate(result2.getCiphertext(), keyPair.getPrivate());

    assertNotNull(secret1);
    assertNotNull(secret2);
  }
}
