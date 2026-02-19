package com.anchorpq.server.crypto;

import com.anchorpq.server.config.CryptoConfig;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.*;
import java.security.*;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.KeyGenerator;
import org.bouncycastle.jcajce.SecretKeyWithEncapsulation;
import org.bouncycastle.jcajce.spec.KEMExtractSpec;
import org.bouncycastle.jcajce.spec.KEMGenerateSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.pqc.jcajce.spec.KyberParameterSpec;

/**
 * Service for ML-KEM (CRYSTALS-Kyber) post-quantum key encapsulation operations.
 *
 * <p>This service manages the server's ML-KEM keypair and provides methods for:
 *
 * <ul>
 *   <li>Key generation at startup
 *   <li>Public key distribution
 *   <li>Shared secret decapsulation from client-provided ciphertexts
 * </ul>
 *
 * <p>Uses Bouncy Castle's PQC provider for ML-KEM implementation.
 */
@ApplicationScoped
@Startup
public class MLKemService {

    private static final String ALGORITHM = "Kyber";
    private static final String PROVIDER = "BCPQC";

    @Inject CryptoConfig cryptoConfig;

    private KeyPair keyPair;
    private String keyId;
    private long keyGeneratedAt;

    static {
        // Register Bouncy Castle providers
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        if (Security.getProvider("BCPQC") == null) {
            Security.addProvider(new BouncyCastlePQCProvider());
        }
    }

    @PostConstruct
    void init() {
        Log.info("Initializing ML-KEM service...");

        String keyFilePath = cryptoConfig.mlkem().keyFilePath().orElse(null);

        if (keyFilePath != null && !keyFilePath.isEmpty()) {
            try {
                loadKeysFromFile(keyFilePath);
                Log.info("ML-KEM keys loaded from file: " + keyFilePath);
                return;
            } catch (Exception e) {
                Log.warn("Failed to load keys from file, generating new keys: " + e.getMessage());
            }
        }

        generateNewKeyPair();

        if (keyFilePath != null && !keyFilePath.isEmpty()) {
            try {
                saveKeysToFile(keyFilePath);
                Log.info("ML-KEM keys saved to file: " + keyFilePath);
            } catch (Exception e) {
                Log.warn("Failed to save keys to file: " + e.getMessage());
            }
        }
    }

    /** Generates a new ML-KEM keypair using the configured parameter set. */
    private void generateNewKeyPair() {
        try {
            String parameterSet = cryptoConfig.mlkem().parameterSet();
            KyberParameterSpec spec = getKyberParameterSpec(parameterSet);

            Log.info("Generating ML-KEM keypair with parameter set: " + parameterSet);

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM, PROVIDER);
            keyPairGenerator.initialize(spec, new SecureRandom());

            this.keyPair = keyPairGenerator.generateKeyPair();
            this.keyId = UUID.randomUUID().toString();
            this.keyGeneratedAt = System.currentTimeMillis();

            Log.info("ML-KEM keypair generated successfully. Key ID: " + keyId);
        } catch (NoSuchAlgorithmException
                | NoSuchProviderException
                | InvalidAlgorithmParameterException e) {
            Log.error("Failed to generate ML-KEM keypair", e);
            throw new CryptoException(
                    CryptoException.ErrorCode.KEY_GENERATION_FAILED,
                    "Failed to generate ML-KEM keypair: " + e.getMessage(),
                    e);
        }
    }

    /** Gets the Kyber parameter specification based on the parameter set name. */
    private KyberParameterSpec getKyberParameterSpec(String parameterSet) {
        return switch (parameterSet.toUpperCase()) {
            case "ML-KEM-512", "KYBER512" -> KyberParameterSpec.kyber512;
            case "ML-KEM-768", "KYBER768" -> KyberParameterSpec.kyber768;
            case "ML-KEM-1024", "KYBER1024" -> KyberParameterSpec.kyber1024;
            default -> {
                Log.warn("Unknown parameter set '" + parameterSet + "', defaulting to ML-KEM-768");
                yield KyberParameterSpec.kyber768;
            }
        };
    }

    /**
     * Returns the server's public key in Base64-encoded format.
     *
     * @return Base64-encoded public key bytes
     */
    public String getPublicKeyBase64() {
        if (keyPair == null) {
            throw new CryptoException(
                    CryptoException.ErrorCode.INVALID_KEY, "ML-KEM keypair not initialized");
        }
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    /**
     * Returns the raw public key bytes.
     *
     * @return Public key bytes
     */
    public byte[] getPublicKeyBytes() {
        if (keyPair == null) {
            throw new CryptoException(
                    CryptoException.ErrorCode.INVALID_KEY, "ML-KEM keypair not initialized");
        }
        return keyPair.getPublic().getEncoded();
    }

    /**
     * Returns the configured ML-KEM parameter set.
     *
     * @return Parameter set name
     */
    public String getParameterSet() {
        return cryptoConfig.mlkem().parameterSet();
    }

    /**
     * Returns the key ID for this keypair.
     *
     * @return Key identifier
     */
    public String getKeyId() {
        return keyId;
    }

    /**
     * Returns the timestamp when the key was generated.
     *
     * @return Key generation timestamp in milliseconds
     */
    public long getKeyGeneratedAt() {
        return keyGeneratedAt;
    }

    /**
     * Decapsulates the shared secret from a client-provided encapsulated key.
     *
     * <p>This is the server-side KEM decapsulation operation. The client encapsulates a shared
     * secret using the server's public key, and the server decapsulates it using its private key.
     *
     * @param encapsulatedKey Base64-encoded encapsulated key (ciphertext) from client
     * @return The shared secret bytes
     * @throws CryptoException if decapsulation fails
     */
    public byte[] decapsulate(String encapsulatedKey) {
        return decapsulate(Base64.getDecoder().decode(encapsulatedKey));
    }

    /**
     * Decapsulates the shared secret from raw encapsulated key bytes.
     *
     * @param encapsulatedKeyBytes Encapsulated key bytes (ciphertext) from client
     * @return The shared secret bytes
     * @throws CryptoException if decapsulation fails
     */
    public byte[] decapsulate(byte[] encapsulatedKeyBytes) {
        if (keyPair == null) {
            throw new CryptoException(
                    CryptoException.ErrorCode.INVALID_KEY, "ML-KEM keypair not initialized");
        }

        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM, PROVIDER);
            KEMExtractSpec extractSpec =
                    new KEMExtractSpec(keyPair.getPrivate(), encapsulatedKeyBytes, "AES");

            keyGenerator.init(extractSpec);
            SecretKeyWithEncapsulation secretKey =
                    (SecretKeyWithEncapsulation) keyGenerator.generateKey();

            byte[] sharedSecret = secretKey.getEncoded();

            Log.debug(
                    "Successfully decapsulated shared secret, length: "
                            + sharedSecret.length
                            + " bytes");

            return sharedSecret;
        } catch (NoSuchAlgorithmException
                | NoSuchProviderException
                | InvalidAlgorithmParameterException e) {
            Log.error("Failed to decapsulate shared secret", e);
            throw new CryptoException(
                    CryptoException.ErrorCode.DECAPSULATION_FAILED,
                    "Failed to decapsulate shared secret",
                    e);
        }
    }

    /**
     * Encapsulates a shared secret using the server's own public key.
     *
     * <p>This is primarily for testing purposes. In production, clients perform encapsulation using
     * the server's public key.
     *
     * @return EncapsulationResult containing the encapsulated key and shared secret
     * @throws CryptoException if encapsulation fails
     */
    public EncapsulationResult encapsulate() {
        return encapsulate(keyPair.getPublic());
    }

    /**
     * Encapsulates a shared secret using a provided public key.
     *
     * @param publicKey The public key to use for encapsulation
     * @return EncapsulationResult containing the encapsulated key and shared secret
     * @throws CryptoException if encapsulation fails
     */
    public EncapsulationResult encapsulate(PublicKey publicKey) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM, PROVIDER);
            KEMGenerateSpec generateSpec = new KEMGenerateSpec(publicKey, "AES");

            keyGenerator.init(generateSpec);
            SecretKeyWithEncapsulation secretKey =
                    (SecretKeyWithEncapsulation) keyGenerator.generateKey();

            byte[] encapsulatedKey = secretKey.getEncapsulation();
            byte[] sharedSecret = secretKey.getEncoded();

            Log.debug(
                    "Successfully encapsulated shared secret, encapsulation length: "
                            + encapsulatedKey.length
                            + " bytes");

            return new EncapsulationResult(encapsulatedKey, sharedSecret);
        } catch (NoSuchAlgorithmException
                | NoSuchProviderException
                | InvalidAlgorithmParameterException e) {
            Log.error("Failed to encapsulate shared secret", e);
            throw new CryptoException(
                    CryptoException.ErrorCode.ENCAPSULATION_FAILED,
                    "Failed to encapsulate shared secret",
                    e);
        }
    }

    /** Result of a KEM encapsulation operation. */
    public record EncapsulationResult(byte[] encapsulatedKey, byte[] sharedSecret) {

        public String getEncapsulatedKeyBase64() {
            return Base64.getEncoder().encodeToString(encapsulatedKey);
        }
    }

    /** Saves the keypair to a file for persistence across restarts. */
    private void saveKeysToFile(String filePath) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(keyPair);
            oos.writeUTF(keyId);
            oos.writeLong(keyGeneratedAt);
        } catch (IOException e) {
            throw new CryptoException(
                    CryptoException.ErrorCode.KEY_SAVE_FAILED,
                    "Failed to save keys to file: " + e.getMessage(),
                    e);
        }
    }

    /** Loads the keypair from a file. */
    private void loadKeysFromFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new CryptoException(
                    CryptoException.ErrorCode.KEY_LOAD_FAILED,
                    "Key file does not exist: " + filePath);
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            this.keyPair = (KeyPair) ois.readObject();
            this.keyId = ois.readUTF();
            this.keyGeneratedAt = ois.readLong();
        } catch (IOException | ClassNotFoundException e) {
            throw new CryptoException(
                    CryptoException.ErrorCode.KEY_LOAD_FAILED,
                    "Failed to load keys from file: " + e.getMessage(),
                    e);
        }
    }
}
