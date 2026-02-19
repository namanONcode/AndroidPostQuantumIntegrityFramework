package com.anchorpq.demo.model

import com.google.gson.annotations.SerializedName

/**
 * Response from the server's /public-key endpoint.
 * Contains the ML-KEM public key for key encapsulation.
 */
data class PublicKeyResponse(
    @SerializedName("publicKey")
    val publicKey: String,

    @SerializedName("parameterSet")
    val parameterSet: String,

    @SerializedName("algorithm")
    val algorithm: String,

    @SerializedName("generatedAt")
    val generatedAt: Long,

    @SerializedName("keyId")
    val keyId: String
)

