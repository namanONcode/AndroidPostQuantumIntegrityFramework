package com.anchorpq.demo.network

import com.anchorpq.demo.model.PublicKeyResponse
import com.anchorpq.demo.model.VerificationRequest
import com.anchorpq.demo.model.VerificationResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit API interface for AnchorPQ server endpoints.
 */
interface AnchorPQApi {

    /**
     * Fetches the server's ML-KEM public key.
     * The public key is used for key encapsulation.
     */
    @GET("/public-key")
    suspend fun getPublicKey(): Response<PublicKeyResponse>

    /**
     * Sends an encrypted verification request to the server.
     *
     * @param request The encrypted verification request containing:
     *                - encapsulatedKey: ML-KEM ciphertext
     *                - encryptedPayload: AES-GCM encrypted integrity data
     * @return Verification response with status (APPROVED/RESTRICTED/REJECTED)
     */
    @POST("/verify")
    suspend fun verify(@Body request: VerificationRequest): Response<VerificationResponse>
}

