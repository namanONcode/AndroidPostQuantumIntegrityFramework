package com.anchorpq.server.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import java.util.Optional;

/** Configuration mapping for AnchorPQ cryptographic settings. */
@ConfigMapping(prefix = "anchorpq.crypto")
public interface CryptoConfig {

    /** ML-KEM specific configuration. */
    MlKemConfig mlkem();

    /** HKDF configuration for key derivation. */
    HkdfConfig hkdf();

    /** AES configuration for symmetric encryption. */
    AesConfig aes();

    interface MlKemConfig {
        /** ML-KEM parameter set to use. Valid values: ML-KEM-512, ML-KEM-768, ML-KEM-1024 */
        @WithDefault("ML-KEM-768")
        String parameterSet();

        /**
         * Optional file path to persist/load ML-KEM keys. If empty, keys are generated at startup
         * and kept in memory only.
         */
        Optional<String> keyFilePath();
    }

    interface HkdfConfig {
        /** Hash algorithm for HKDF key derivation. */
        @WithDefault("SHA3-256")
        String algorithm();

        /** Context info string for HKDF. */
        @WithDefault("AnchorPQ-v1-IntegrityVerification")
        String info();
    }

    interface AesConfig {
        /** AES key size in bits. */
        @WithDefault("256")
        int keySize();

        /** AES-GCM IV size in bytes. */
        @WithDefault("12")
        int ivSize();

        /** AES-GCM authentication tag size in bits. */
        @WithDefault("128")
        int tagSize();
    }
}
