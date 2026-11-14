package com.mfsrelay.app.data.remote

import com.google.gson.Gson
import com.mfsrelay.app.data.remote.dto.ErrorResponse
import retrofit2.Response

private val gson = Gson()

/** Best-effort parse of a non-2xx Retrofit [Response]'s error body into [ErrorResponse]. */
fun <T> parseErrorBody(response: Response<T>): ErrorResponse {
    val raw = response.errorBody()?.string()
    if (raw.isNullOrBlank()) {
        return ErrorResponse(
            success = false,
            errorCode = "http_${response.code()}",
            message = response.message().ifBlank { "HTTP ${response.code()}" }
        )
    }
    return runCatching { gson.fromJson(raw, ErrorResponse::class.java) }
        .getOrNull()
        ?: ErrorResponse(success = false, errorCode = "http_${response.code()}", message = raw)
}
