package com.anchorpq.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Response containing the server's ML-KEM public key for client key encapsulation. */
public class PublicKeyResponse {

    /** The ML-KEM public key in Base64-encoded format. */
    @JsonProperty("publicKey")
    private String publicKey;

    /** The ML-KEM parameter set used (e.g., "ML-KEM-768"). */
    @JsonProperty("parameterSet")
    private String parameterSet;

    /** Algorithm identifier. */
    @JsonProperty("algorithm")
    private String algorithm;

    /** Key generation timestamp. */
    @JsonProperty("generatedAt")
    private Long generatedAt;

    /** Optional key identifier for key rotation support. */
    @JsonProperty("keyId")
    private String keyId;

    public PublicKeyResponse() {}

    public PublicKeyResponse(
            String publicKey,
            String parameterSet,
            String algorithm,
            Long generatedAt,
            String keyId) {
        this.publicKey = publicKey;
        this.parameterSet = parameterSet;
        this.algorithm = algorithm;
        this.generatedAt = generatedAt;
        this.keyId = keyId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getParameterSet() {
        return parameterSet;
    }

    public void setParameterSet(String parameterSet) {
        this.parameterSet = parameterSet;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public Long getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Long generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    @Override
    public String toString() {
        return "PublicKeyResponse{"
                + "parameterSet='"
                + parameterSet
                + '\''
                + ", algorithm='"
                + algorithm
                + '\''
                + ", generatedAt="
                + generatedAt
                + ", keyId='"
                + keyId
                + '\''
                + '}';
    }
}
