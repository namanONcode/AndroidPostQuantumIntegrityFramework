package com.anchorpq.demo.network

import android.util.Base64
import android.util.Log
import com.anchorpq.demo.crypto.IntegrityEncryptionService
import com.anchorpq.demo.model.VerificationResponse
import com.anchorpq.demo.model.VerificationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository class that handles the complete integrity verification flow:
 * 1. Fetch server's ML-KEM public key
 * 2. Create encrypted verification request
 * 3. Send request and receive response
 */
class IntegrityRepository(private val serverUrl: String) {

    companion object {
        private const val TAG = "IntegrityRepository"
    }

    private val api = ApiClient.getApi(serverUrl)
    private val encryptionService = IntegrityEncryptionService()

    /**
     * Result of the verification process.
     */
    sealed class VerificationResult {
        data class Success(val response: VerificationResponse) : VerificationResult()
        data class Error(val message: String, val cause: Throwable? = null) : VerificationResult()
    }

    /**
     * Progress callback for UI updates.
     */
    interface ProgressCallback {
        fun onProgress(step: VerificationStep)
    }

    /**
     * Verification steps for progress reporting.
     */
    enum class VerificationStep {
        FETCHING_PUBLIC_KEY,
        ENCRYPTING_PAYLOAD,
        SENDING_REQUEST,
        PROCESSING_RESPONSE,
        COMPLETE
    }

    /**
     * Performs the complete integrity verification flow.
     *
     * @param merkleRoot The application's Merkle root hash
     * @param version Application version
     * @param variant Build variant
     * @param progressCallback Optional callback for progress updates
     * @return VerificationResult with success or error
     */
    suspend fun verifyIntegrity(
        merkleRoot: String,
        version: String,
        variant: String,
        progressCallback: ProgressCallback? = null
    ): VerificationResult = withContext(Dispatchers.IO) {
        try {
            // Step 1: Fetch server's public key
            Log.d(TAG, "Step 1: Fetching server public key from $serverUrl")
            progressCallback?.onProgress(VerificationStep.FETCHING_PUBLIC_KEY)

            val publicKeyResponse = api.getPublicKey()
            if (!publicKeyResponse.isSuccessful) {
                val errorBody = publicKeyResponse.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Failed to fetch public key: ${publicKeyResponse.code()} - $errorBody")
                return@withContext VerificationResult.Error(
                    "Failed to fetch server public key: ${publicKeyResponse.code()}"
                )
            }

            val publicKeyData = publicKeyResponse.body()
                ?: return@withContext VerificationResult.Error("Empty public key response")

            Log.d(TAG, "Received public key: algorithm=${publicKeyData.algorithm}, " +
                      "parameterSet=${publicKeyData.parameterSet}, keyId=${publicKeyData.keyId}")

            // Decode the public key
            val publicKeyBytes = Base64.decode(publicKeyData.publicKey, Base64.NO_WRAP)

            // Step 2: Create encrypted verification request
            Log.d(TAG, "Step 2: Creating encrypted verification request")
            progressCallback?.onProgress(VerificationStep.ENCRYPTING_PAYLOAD)

            val verificationRequest = encryptionService.createVerificationRequest(
                merkleRoot = merkleRoot,
                version = version,
                variant = variant,
                serverPublicKeyBytes = publicKeyBytes
            )

            Log.d(TAG, "Created encrypted request: encapsulatedKey length=${verificationRequest.encapsulatedKey.length}, " +
                      "encryptedPayload length=${verificationRequest.encryptedPayload.length}")

            // Step 3: Send verification request
            Log.d(TAG, "Step 3: Sending verification request to server")
            progressCallback?.onProgress(VerificationStep.SENDING_REQUEST)

            val verificationResponse = api.verify(verificationRequest)

            // Step 4: Process response
            Log.d(TAG, "Step 4: Processing server response")
            progressCallback?.onProgress(VerificationStep.PROCESSING_RESPONSE)

            if (!verificationResponse.isSuccessful) {
                val errorBody = verificationResponse.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Verification request failed: ${verificationResponse.code()} - $errorBody")
                return@withContext VerificationResult.Error(
                    "Verification failed: ${verificationResponse.code()}"
                )
            }

            val response = verificationResponse.body()
                ?: return@withContext VerificationResult.Error("Empty verification response")

            Log.d(TAG, "Verification complete: status=${response.status}, message=${response.message}")
            progressCallback?.onProgress(VerificationStep.COMPLETE)

            VerificationResult.Success(response)

        } catch (e: java.net.ConnectException) {
            Log.e(TAG, "Connection failed", e)
            VerificationResult.Error("Cannot connect to server. Is the server running?", e)
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Connection timeout", e)
            VerificationResult.Error("Connection timeout. Please try again.", e)
        } catch (e: java.security.GeneralSecurityException) {
            Log.e(TAG, "Encryption error", e)
            VerificationResult.Error("Encryption failed: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error", e)
            VerificationResult.Error("Unexpected error: ${e.message}", e)
        }
    }
}

