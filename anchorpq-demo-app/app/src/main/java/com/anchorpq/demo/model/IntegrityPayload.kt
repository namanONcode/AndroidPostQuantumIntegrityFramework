package com.anchorpq.demo.model

import com.google.gson.annotations.SerializedName

/**
 * Integrity payload sent to the server (encrypted).
 * Contains the Merkle root and application metadata.
 */
data class IntegrityPayload(
    @SerializedName("merkleRoot")
    val merkleRoot: String,

    @SerializedName("version")
    val version: String,

    @SerializedName("variant")
    val variant: String,

    @SerializedName("timestamp")
    val timestamp: String,

    @SerializedName("nonce")
    val nonce: String,

    @SerializedName("signerFingerprint")
    val signerFingerprint: String? = null,

    @SerializedName("device")
    val device: DeviceInfo? = null
)

/**
 * Device information included in the integrity payload.
 */
data class DeviceInfo(
    @SerializedName("sdk")
    val sdk: String,

    @SerializedName("model")
    val model: String,

    @SerializedName("manufacturer")
    val manufacturer: String
)

