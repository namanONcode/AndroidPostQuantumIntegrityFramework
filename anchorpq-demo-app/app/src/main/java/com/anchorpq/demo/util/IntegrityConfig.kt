package com.anchorpq.demo.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.InputStreamReader

/**
 * Utility class for loading integrity configuration from assets.
 *
 * The AnchorPQ plugin generates an integrity.json file that contains
 * the Merkle root and other metadata. This class loads that file.
 */
data class IntegrityConfig(
    @SerializedName("merkleRoot")
    val merkleRoot: String,

    @SerializedName("version")
    val version: String,

    @SerializedName("variant")
    val variant: String,

    @SerializedName("hashAlgorithm")
    val algorithm: String? = null,

    @SerializedName("timestamp")
    val timestamp: String? = null,

    @SerializedName("signerFingerprint")
    val signerFingerprint: String? = null,

    @SerializedName("leafCount")
    val leafCount: Int? = null
) {
    companion object {
        private const val INTEGRITY_FILE = "integrity.json"
        private val gson = Gson()

        /**
         * Loads the integrity configuration from the assets folder.
         *
         * @param context Android context
         * @return IntegrityConfig or null if not found
         */
        fun loadFromAssets(context: Context): IntegrityConfig? {
            return try {
                context.assets.open(INTEGRITY_FILE).use { inputStream ->
                    InputStreamReader(inputStream).use { reader ->
                        gson.fromJson(reader, IntegrityConfig::class.java)
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}

