package com.mfsrelay.app.data.remote

import com.mfsrelay.app.data.remote.dto.PingResponse
import com.mfsrelay.app.data.remote.dto.SmsWebhookRequest
import com.mfsrelay.app.data.remote.dto.SmsWebhookResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit interface for the WordPress/WooCommerce plugin's REST API, as defined in
 * API_CONTRACT.md. Paths, headers and JSON field names here MUST stay in sync with that file.
 *
 * Auth headers (X-MFS-API-Key / X-MFS-Device-Id / Content-Type) are added by
 * [AuthHeaderInterceptor], not declared per-method here, so they can be sourced live from
 * Settings without rebuilding the Retrofit client on every change.
 */
interface MfsApiService {

    @GET("wp-json/mfs/v1/ping")
    suspend fun ping(): Response<PingResponse>

    @POST("wp-json/mfs/v1/sms-webhook")
    suspend fun submitSms(@Body request: SmsWebhookRequest): Response<SmsWebhookResponse>
}
