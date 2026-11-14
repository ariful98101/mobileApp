package com.mfsrelay.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * GET /wp-json/mfs/v1/ping — success body.
 * On 401/403 the server instead returns an [ErrorResponse]; Retrofit surfaces that as an
 * HttpException which the caller converts via [com.mfsrelay.app.data.remote.parseErrorBody].
 */
data class PingResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("device_label") val deviceLabel: String?,
    @SerializedName("server_time") val serverTime: String?
)
