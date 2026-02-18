package com.anchorpq.runtime;

import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.bouncycastle.jcajce.SecretKeyWithEncapsulation;
import org.bouncycastle.jcajce.spec.KEMExtractSpec;
import org.bouncycastle.jcajce.spec.KEMGenerateSpec;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.pqc.jcajce.spec.KyberParameterSpec;

/**
 * ML-KEM (CRYSTALS-Kyber) key encapsulation mechanism helper.
 *
 * <p>This class provides: - Key pair generation for ML-KEM - Key encapsulation (sender side) - Key
 * decapsulation (receiver side) - Hybrid encryption using ML-KEM + AES-GCM
 *
 * <p>Security Level: Kyber-768 (equivalent to AES-192)
 *
 * <p>Usage:
 *
 * <pre>
 * MLKemHelper serverHelper = new MLKemHelper();
 * KeyPair serverKeys = serverHelper.generateKeyPair();
 * byte[] publicKey = serverKeys.getPublic().getEncoded();
 *
 * MLKemHelper clientHelper = new MLKemHelper();
 * EncapsulationResult result = clientHelper.encapsulate(publicKey);
 * byte[] ciphertext = result.getCiphertext();
 * SecretKey sharedSecret = result.getSharedSecret();
 *
 * SecretKey serverSharedSecret = serverHelper.decapsulate(ciphertext, serverKeys.getPrivate());
 *
 * </pre>
 */
public class MLKemHelper {

  private static final String KEM_ALGORITHM = "Kyber";
  private static final String PROVIDER = "BCPQC";
  private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
  private static final int GCM_TAG_LENGTH = 128;
  private static final int GCM_NONCE_LENGTH = 12;

  static {
    if (Security.getProvider(PROVIDER) == null) {
      Security.addProvider(new BouncyCastlePQCProvider());
    }
  }

  private final KyberParameterSpec parameterSpec;
  private final SecureRandom secureRandom;

  /** Creates a new MLKemHelper with Kyber-768 parameters. */
  public MLKemHelper() {
    this(KyberParameterSpec.kyber768);
  }

  /**
   * Creates a new MLKemHelper with specified parameters.
   *
   * @param parameterSpec Kyber parameter specification
   */
  public MLKemHelper(KyberParameterSpec parameterSpec) {
    this.parameterSpec = parameterSpec;
    this.secureRandom = new SecureRandom();
  }

  /**
   * Generates an ML-KEM key pair.
   *
   * @return the generated key pair
   * @throws GeneralSecurityException if key generation fails
   */
  public KeyPair generateKeyPair() throws GeneralSecurityException {
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEM_ALGORITHM, PROVIDER);
    keyPairGenerator.initialize(parameterSpec, secureRandom);
    return keyPairGenerator.generateKeyPair();
  }

  /**
   * Encapsulates a shared secret using the recipient's public key.
   *
   * @param publicKey the recipient's public key (encoded)
   * @return encapsulation result containing ciphertext and shared secret
   * @throws GeneralSecurityException if encapsulation fails
   */
  public EncapsulationResult encapsulate(byte[] publicKey) throws GeneralSecurityException {
    KeyFactory keyFactory = KeyFactory.getInstance(KEM_ALGORITHM, PROVIDER);
    PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKey));

    return encapsulate(pubKey);
  }

  /**
   * Encapsulates a shared secret using the recipient's public key.
   *
   * @param publicKey the recipient's public key
   * @return encapsulation result containing ciphertext and shared secret
   * @throws GeneralSecurityException if encapsulation fails
   */
  public EncapsulationResult encapsulate(PublicKey publicKey) throws GeneralSecurityException {
    KeyGenerator keyGenerator = KeyGenerator.getInstance(KEM_ALGORITHM, PROVIDER);
    keyGenerator.init(new KEMGenerateSpec(publicKey, "AES"), secureRandom);

    SecretKeyWithEncapsulation secretKey = (SecretKeyWithEncapsulation) keyGenerator.generateKey();

    return new EncapsulationResult(secretKey.getEncapsulation(), secretKey);
  }

  /**
   * Decapsulates the shared secret using the private key.
   *
   * @param ciphertext the encapsulation ciphertext
   * @param privateKey the recipient's private key
   * @return the shared secret
   * @throws GeneralSecurityException if decapsulation fails
   */
  public SecretKey decapsulate(byte[] ciphertext, PrivateKey privateKey)
      throws GeneralSecurityException {
    KeyGenerator keyGenerator = KeyGenerator.getInstance(KEM_ALGORITHM, PROVIDER);
    keyGenerator.init(new KEMExtractSpec(privateKey, ciphertext, "AES"), secureRandom);

    return keyGenerator.generateKey();
  }

  /**
   * Encrypts data using the shared secret (AES-GCM).
   *
   * @param plaintext the data to encrypt
   * @param sharedSecret the shared secret from encapsulation
   * @return encrypted data with prepended nonce
   * @throws GeneralSecurityException if encryption fails
   */
  public byte[] encrypt(byte[] plaintext, SecretKey sharedSecret) throws GeneralSecurityException {
    byte[] nonce = new byte[GCM_NONCE_LENGTH];
    secureRandom.nextBytes(nonce);

    Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
    cipher.init(Cipher.ENCRYPT_MODE, sharedSecret, new GCMParameterSpec(GCM_TAG_LENGTH, nonce));

    byte[] ciphertext = cipher.doFinal(plaintext);

    byte[] result = new byte[nonce.length + ciphertext.length];
    System.arraycopy(nonce, 0, result, 0, nonce.length);
    System.arraycopy(ciphertext, 0, result, nonce.length, ciphertext.length);

    return result;
  }

  /**
   * Decrypts data using the shared secret (AES-GCM).
   *
   * @param encryptedData the encrypted data with prepended nonce
   * @param sharedSecret the shared secret from decapsulation
   * @return decrypted plaintext
   * @throws GeneralSecurityException if decryption fails
   */
  public byte[] decrypt(byte[] encryptedData, SecretKey sharedSecret)
      throws GeneralSecurityException {
    if (encryptedData.length < GCM_NONCE_LENGTH) {
      throw new IllegalArgumentException("Encrypted data too short");
    }

    byte[] nonce = new byte[GCM_NONCE_LENGTH];
    System.arraycopy(encryptedData, 0, nonce, 0, nonce.length);

    byte[] ciphertext = new byte[encryptedData.length - nonce.length];
    System.arraycopy(encryptedData, nonce.length, ciphertext, 0, ciphertext.length);

    Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
    cipher.init(Cipher.DECRYPT_MODE, sharedSecret, new GCMParameterSpec(GCM_TAG_LENGTH, nonce));

    return cipher.doFinal(ciphertext);
  }

  /**
   * Performs hybrid encryption: ML-KEM key exchange + AES-GCM encryption.
   *
   * @param plaintext the data to encrypt
   * @param recipientPublicKey the recipient's ML-KEM public key
   * @return hybrid encrypted payload
   * @throws GeneralSecurityException if encryption fails
   */
  public HybridEncryptedPayload hybridEncrypt(byte[] plaintext, PublicKey recipientPublicKey)
      throws GeneralSecurityException {
    EncapsulationResult encap = encapsulate(recipientPublicKey);

    byte[] encryptedData = encrypt(plaintext, encap.getSharedSecret());

    return new HybridEncryptedPayload(encap.getCiphertext(), encryptedData);
  }

  /**
   * Performs hybrid decryption.
   *
   * @param payload the hybrid encrypted payload
   * @param recipientPrivateKey the recipient's ML-KEM private key
   * @return decrypted plaintext
   * @throws GeneralSecurityException if decryption fails
   */
  public byte[] hybridDecrypt(HybridEncryptedPayload payload, PrivateKey recipientPrivateKey)
      throws GeneralSecurityException {
    SecretKey sharedSecret = decapsulate(payload.getKemCiphertext(), recipientPrivateKey);

    return decrypt(payload.getEncryptedData(), sharedSecret);
  }

  /** Result of key encapsulation. */
  public static class EncapsulationResult {
    private final byte[] ciphertext;
    private final SecretKey sharedSecret;

    public EncapsulationResult(byte[] ciphertext, SecretKey sharedSecret) {
      this.ciphertext = ciphertext.clone();
      this.sharedSecret = sharedSecret;
    }

    public byte[] getCiphertext() {
      return ciphertext.clone();
    }

    public SecretKey getSharedSecret() {
      return sharedSecret;
    }

    public String getCiphertextBase64() {
      return Base64.getEncoder().encodeToString(ciphertext);
    }
  }

  /** Hybrid encrypted payload containing KEM ciphertext and encrypted data. */
  public static class HybridEncryptedPayload {
    private final byte[] kemCiphertext;
    private final byte[] encryptedData;

    public HybridEncryptedPayload(byte[] kemCiphertext, byte[] encryptedData) {
      this.kemCiphertext = kemCiphertext.clone();
      this.encryptedData = encryptedData.clone();
    }

    public byte[] getKemCiphertext() {
      return kemCiphertext.clone();
    }

    public byte[] getEncryptedData() {
      return encryptedData.clone();
    }

    /**
     * Serializes the payload to bytes. Format: [4 bytes KEM length][KEM ciphertext][encrypted data]
     */
    public byte[] toBytes() {
      byte[] result = new byte[4 + kemCiphertext.length + encryptedData.length];

      result[0] = (byte) (kemCiphertext.length >> 24);
      result[1] = (byte) (kemCiphertext.length >> 16);
      result[2] = (byte) (kemCiphertext.length >> 8);
      result[3] = (byte) kemCiphertext.length;

      System.arraycopy(kemCiphertext, 0, result, 4, kemCiphertext.length);

      System.arraycopy(encryptedData, 0, result, 4 + kemCiphertext.length, encryptedData.length);

      return result;
    }

    /** Deserializes payload from bytes. */
    public static HybridEncryptedPayload fromBytes(byte[] data) {
      if (data.length < 4) {
        throw new IllegalArgumentException("Data too short");
      }

      int kemLength =
          ((data[0] & 0xFF) << 24)
              | ((data[1] & 0xFF) << 16)
              | ((data[2] & 0xFF) << 8)
              | (data[3] & 0xFF);

      if (data.length < 4 + kemLength) {
        throw new IllegalArgumentException("Invalid KEM length");
      }

      byte[] kemCiphertext = new byte[kemLength];
      System.arraycopy(data, 4, kemCiphertext, 0, kemLength);

      byte[] encryptedData = new byte[data.length - 4 - kemLength];
      System.arraycopy(data, 4 + kemLength, encryptedData, 0, encryptedData.length);

      return new HybridEncryptedPayload(kemCiphertext, encryptedData);
    }

    public String toBase64() {
      return Base64.getEncoder().encodeToString(toBytes());
    }

    public static HybridEncryptedPayload fromBase64(String base64) {
      return fromBytes(Base64.getDecoder().decode(base64));
    }
  }
}
