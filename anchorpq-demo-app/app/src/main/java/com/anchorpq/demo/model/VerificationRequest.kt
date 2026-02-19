package com.anchorpq.demo.model

import com.google.gson.annotations.SerializedName

/**
 * Request sent to the server's /verify endpoint.
 * Contains the ML-KEM encapsulated key and encrypted integrity payload.
 */
data class VerificationRequest(
    @SerializedName("encapsulatedKey")
    val encapsulatedKey: String,

    @SerializedName("encryptedPayload")
    val encryptedPayload: String,

    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @SerializedName("nonce")
    val nonce: String? = null
)

