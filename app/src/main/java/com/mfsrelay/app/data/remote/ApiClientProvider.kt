package com.mfsrelay.app.data.remote

import com.mfsrelay.app.settings.SettingsManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds (and caches, per base URL) the Retrofit client for [MfsApiService].
 *
 * The server base URL is user-configurable at runtime (Settings screen), so this is not a
 * static singleton keyed only on the Application context - [get] rebuilds the Retrofit
 * instance whenever the configured base URL changes. The API key / device id, which change
 * less disruptively, are read fresh on every request by [AuthHeaderInterceptor] instead of
 * baking them into the client.
 */
class ApiClientProvider(private val settingsManager: SettingsManager) {

    @Volatile private var cachedBaseUrl: String? = null
    @Volatile private var cachedService: MfsApiService? = null

    suspend fun get(): MfsApiService? {
        val baseUrl = settingsManager.getServerBaseUrl()
        if (baseUrl.isBlank()) return null

        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        cachedService?.let { if (cachedBaseUrl == normalized) return it }

        synchronized(this) {
            cachedService?.let { if (cachedBaseUrl == normalized) return it }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(AuthHeaderInterceptor(settingsManager))
                .addInterceptor(
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
                )
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(normalized)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(MfsApiService::class.java)
            cachedBaseUrl = normalized
            cachedService = service
            return service
        }
    }
}

/** Adds the three required headers to every request, read live from [SettingsManager]. */
private class AuthHeaderInterceptor(private val settingsManager: SettingsManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // Interceptors run on OkHttp's dispatcher thread, never the main thread, so a blocking
        // read of the (tiny, memory-cached-by-DataStore) settings here is safe.
        val apiKey = runBlocking { settingsManager.getApiKey() }
        val deviceId = runBlocking { settingsManager.getOrCreateDeviceId() }

        val newRequest = chain.request().newBuilder()
            .addHeader("X-MFS-API-Key", apiKey)
            .addHeader("X-MFS-Device-Id", deviceId)
            .addHeader("Content-Type", "application/json")
            .build()

        return chain.proceed(newRequest)
    }
}
