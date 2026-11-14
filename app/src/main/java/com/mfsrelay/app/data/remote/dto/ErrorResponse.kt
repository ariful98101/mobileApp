package com.mfsrelay.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Shared shape for any `{ "success": false, ... }` error body from the API. */
data class ErrorResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("client_submission_id") val clientSubmissionId: String? = null,
    @SerializedName("error_code") val errorCode: String? = null,
    @SerializedName("message") val message: String? = null
)

/** Known error codes from API_CONTRACT.md section 4. Extend as the server side extends it. */
object ApiErrorCodes {
    const val INVALID_API_KEY = "invalid_api_key"
    const val MISSING_API_KEY = "missing_api_key"
    const val DEVICE_REVOKED = "device_revoked"
    const val MISSING_TRX_ID = "missing_trx_id"
    const val INVALID_METHOD = "invalid_method"
    const val INVALID_PAYLOAD = "invalid_payload"
    const val RATE_LIMITED = "rate_limited"
}
