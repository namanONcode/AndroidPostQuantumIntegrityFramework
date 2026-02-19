package com.anchorpq.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Represents the verification response returned to the Android client. */
public class VerificationResponse {

    /** Verification status enumeration. */
    public enum Status {
        /** Integrity verified successfully - application is authentic. */
        APPROVED,

        /**
         * Integrity partially verified - some discrepancies detected. Application may be allowed
         * with restricted features.
         */
        RESTRICTED,

        /** Integrity verification failed - application tampered or unknown. */
        REJECTED
    }

    /** The verification status. */
    @JsonProperty("status")
    private Status status;

    /** Human-readable message describing the verification result. */
    @JsonProperty("message")
    private String message;

    /** Timestamp of the verification decision. */
    @JsonProperty("timestamp")
    private Long timestamp;

    /** Optional error code for debugging (only in non-production). */
    @JsonProperty("errorCode")
    private String errorCode;

    public VerificationResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public VerificationResponse(Status status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public static VerificationResponse approved() {
        return new VerificationResponse(Status.APPROVED, "Integrity verified successfully");
    }

    public static VerificationResponse approved(String message) {
        return new VerificationResponse(Status.APPROVED, message);
    }

    public static VerificationResponse restricted(String message) {
        return new VerificationResponse(Status.RESTRICTED, message);
    }

    public static VerificationResponse rejected(String message) {
        return new VerificationResponse(Status.REJECTED, message);
    }

    public static VerificationResponse rejected(String message, String errorCode) {
        VerificationResponse response = new VerificationResponse(Status.REJECTED, message);
        response.setErrorCode(errorCode);
        return response;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public String toString() {
        return "VerificationResponse{"
                + "status="
                + status
                + ", message='"
                + message
                + '\''
                + ", timestamp="
                + timestamp
                + ", errorCode='"
                + errorCode
                + '\''
                + '}';
    }
}
