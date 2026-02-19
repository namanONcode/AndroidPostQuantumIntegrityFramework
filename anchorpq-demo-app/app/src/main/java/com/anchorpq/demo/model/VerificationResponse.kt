package com.anchorpq.demo.model

import com.google.gson.annotations.SerializedName

/**
 * Response from the server's /verify endpoint.
 * Contains the verification decision.
 */
data class VerificationResponse(
    @SerializedName("status")
    val status: VerificationStatus,

    @SerializedName("message")
    val message: String,

    @SerializedName("timestamp")
    val timestamp: Long,

    @SerializedName("errorCode")
    val errorCode: String? = null
)

/**
 * Verification status enum matching server-side status values.
 */
enum class VerificationStatus {
    @SerializedName("APPROVED")
    APPROVED,

    @SerializedName("RESTRICTED")
    RESTRICTED,

    @SerializedName("REJECTED")
    REJECTED
}

