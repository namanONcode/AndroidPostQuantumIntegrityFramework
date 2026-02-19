package com.anchorpq.server.crypto;

/** Exception thrown when cryptographic operations fail. */
public class CryptoException extends RuntimeException {

    private final ErrorCode errorCode;

    public enum ErrorCode {
        KEY_GENERATION_FAILED("CRYPTO_001", "Failed to generate cryptographic keys"),
        ENCAPSULATION_FAILED("CRYPTO_002", "Failed to encapsulate shared secret"),
        DECAPSULATION_FAILED("CRYPTO_003", "Failed to decapsulate shared secret"),
        KEY_DERIVATION_FAILED("CRYPTO_004", "Failed to derive encryption key"),
        ENCRYPTION_FAILED("CRYPTO_005", "Failed to encrypt data"),
        DECRYPTION_FAILED("CRYPTO_006", "Failed to decrypt data"),
        INVALID_CIPHERTEXT("CRYPTO_007", "Invalid or corrupted ciphertext"),
        AUTHENTICATION_FAILED("CRYPTO_008", "Authentication tag verification failed"),
        INVALID_KEY("CRYPTO_009", "Invalid cryptographic key"),
        PROVIDER_ERROR("CRYPTO_010", "Cryptographic provider error"),
        KEY_LOAD_FAILED("CRYPTO_011", "Failed to load cryptographic keys from file"),
        KEY_SAVE_FAILED("CRYPTO_012", "Failed to save cryptographic keys to file");

        private final String code;
        private final String description;

        ErrorCode(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    public CryptoException(ErrorCode errorCode) {
        super(errorCode.getDescription());
        this.errorCode = errorCode;
    }

    public CryptoException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public CryptoException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getDescription(), cause);
        this.errorCode = errorCode;
    }

    public CryptoException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getErrorCodeString() {
        return errorCode.getCode();
    }
}
