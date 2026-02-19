package com.anchorpq.demo.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton object providing configured Retrofit instance for AnchorPQ API.
 */
object ApiClient {

    private const val CONNECT_TIMEOUT = 30L
    private const val READ_TIMEOUT = 30L
    private const val WRITE_TIMEOUT = 30L

    private var retrofit: Retrofit? = null
    private var baseUrl: String = ""

    /**
     * Gets or creates the AnchorPQ API instance.
     *
     * @param baseUrl The base URL of the AnchorPQ server
     * @return Configured AnchorPQApi instance
     */
    fun getApi(baseUrl: String): AnchorPQApi {
        if (retrofit == null || this.baseUrl != baseUrl) {
            this.baseUrl = baseUrl
            retrofit = createRetrofit(baseUrl)
        }
        return retrofit!!.create(AnchorPQApi::class.java)
    }

    private fun createRetrofit(baseUrl: String): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .addHeader("User-Agent", "AnchorPQ-Demo-Android/1.0")
                    .build()
                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Clears the cached Retrofit instance.
     * Useful for testing or when changing server URLs.
     */
    fun reset() {
        retrofit = null
        baseUrl = ""
    }
}

