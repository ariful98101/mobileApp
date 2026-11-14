package com.mfsrelay.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local record of one captured MFS payment SMS and the state of its upload to the
 * WordPress/WooCommerce plugin's REST API.
 *
 * [id] (a UUID string) is also used, unchanged, as the `client_submission_id` sent to the
 * server - this lets the server's idempotency rule on (method, trx_id) line up with a single
 * stable local row even across retries.
 */
@Entity(tableName = "sms_log_entries")
data class SmsLogEntry(
    @PrimaryKey
    val id: String,

    /** One of "bkash", "rocket", "nagad". */
    val method: String,

    /** Raw SMS sender id / shortcode. */
    val sender: String,

    /** Payer's phone number extracted from the SMS text, if present. */
    val customerPhone: String?,

    /** Extracted transaction/reference id. Required by the server - half of the match key. */
    val trxId: String?,

    /** Extracted amount, if parseable. */
    val amount: Double?,

    /** Full, unmodified original SMS body. */
    val rawMessage: String,

    /** Epoch millis, device-local time the SMS arrived. */
    val receivedAtEpochMillis: Long,

    val status: UploadStatus = UploadStatus.PENDING_UPLOAD,

    /** Free-form text from the server's last response (match info, or error message). */
    val serverMessage: String? = null,

    /** Structured error code from the server's last failed response, e.g. "missing_trx_id". */
    val errorCode: String? = null,

    /** Set once the server reports a match. */
    val matched: Boolean = false,
    val matchedPaymentId: Long? = null,
    val matchedOrderId: Long? = null,

    val lastAttemptAtEpochMillis: Long? = null,
    val attemptCount: Int = 0
)

enum class UploadStatus {
    PENDING_UPLOAD,
    SUBMITTED,
    FAILED,
    MATCHED,
    UNMATCHED
}
