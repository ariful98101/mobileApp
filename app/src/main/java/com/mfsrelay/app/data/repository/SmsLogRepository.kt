package com.mfsrelay.app.data.repository

import com.mfsrelay.app.data.local.SmsLogDao
import com.mfsrelay.app.data.local.SmsLogEntry
import com.mfsrelay.app.data.local.UploadStatus
import com.mfsrelay.app.data.remote.ApiClientProvider
import com.mfsrelay.app.data.remote.dto.SmsWebhookRequest
import com.mfsrelay.app.data.remote.parseErrorBody
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Result of one upload attempt, used by [com.mfsrelay.app.work.SmsUploadWorker] to decide retry vs give up. */
sealed class UploadResult {
    data object Success : UploadResult()
    /** Server rejected the request (4xx/401/403) - permanent failure, do not retry automatically. */
    data class PermanentFailure(val errorCode: String?, val message: String?) : UploadResult()
    /** Network/timeout/5xx - transient, caller should let WorkManager retry with backoff. */
    data class TransientFailure(val message: String?) : UploadResult()
}

class SmsLogRepository(
    private val dao: SmsLogDao,
    private val apiClientProvider: ApiClientProvider
) {

    fun observeAll(): Flow<List<SmsLogEntry>> = dao.observeAll()

    suspend fun insert(entry: SmsLogEntry) = dao.insert(entry)

    suspend fun getById(id: String): SmsLogEntry? = dao.getById(id)

    suspend fun getFailedEntries(): List<SmsLogEntry> = dao.getByStatus(UploadStatus.FAILED)

    suspend fun getPendingEntries(): List<SmsLogEntry> = dao.getByStatus(UploadStatus.PENDING_UPLOAD)

    /**
     * Marks [id] back to PENDING_UPLOAD so a freshly enqueued WorkManager job will pick it up.
     * Reuses the same row (and therefore the same `client_submission_id`), which is what lets
     * the server's (method, trx_id) idempotency rule recognize this as a retry rather than a
     * new submission.
     */
    suspend fun markPendingForRetry(id: String) {
        val entry = dao.getById(id) ?: return
        dao.update(entry.copy(status = UploadStatus.PENDING_UPLOAD))
    }

    /** Performs the actual network call for row [id] and persists the outcome. Returns the outcome for the caller (Worker) to act on. */
    suspend fun attemptUpload(id: String): UploadResult {
        val entry = dao.getById(id) ?: return UploadResult.PermanentFailure("local_row_missing", "Local row no longer exists")

        val service = apiClientProvider.get()
            ?: return UploadResult.TransientFailure("Server URL not configured yet")

        val attemptTime = System.currentTimeMillis()

        val request = SmsWebhookRequest(
            method = entry.method,
            sender = entry.sender,
            customerPhone = entry.customerPhone,
            trxId = entry.trxId,
            amount = entry.amount,
            rawMessage = entry.rawMessage,
            receivedAt = isoFormat(entry.receivedAtEpochMillis),
            clientSubmissionId = entry.id
        )

        return try {
            val response = service.submitSms(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body == null || !body.success) {
                    val updated = entry.copy(
                        status = UploadStatus.FAILED,
                        serverMessage = "Empty/invalid success response from server",
                        lastAttemptAtEpochMillis = attemptTime,
                        attemptCount = entry.attemptCount + 1
                    )
                    dao.update(updated)
                    UploadResult.TransientFailure("Empty/invalid success response from server")
                } else {
                    // matched:true -> MATCHED; matched:false -> SUBMITTED (stored server-side,
                    // the server's 2-min cron keeps trying to match it - see API_CONTRACT.md §3).
                    val newStatus = if (body.matched) UploadStatus.MATCHED else UploadStatus.SUBMITTED
                    val updated = entry.copy(
                        status = newStatus,
                        serverMessage = body.message,
                        matched = body.matched,
                        matchedPaymentId = body.paymentId,
                        matchedOrderId = body.orderId,
                        errorCode = null,
                        lastAttemptAtEpochMillis = attemptTime,
                        attemptCount = entry.attemptCount + 1
                    )
                    dao.update(updated)
                    UploadResult.Success
                }
            } else {
                val error = parseErrorBody(response)
                val isPermanent = response.code() in 400..499
                val updated = entry.copy(
                    status = UploadStatus.FAILED,
                    serverMessage = error.message,
                    errorCode = error.errorCode,
                    lastAttemptAtEpochMillis = attemptTime,
                    attemptCount = entry.attemptCount + 1
                )
                dao.update(updated)
                if (isPermanent) {
                    UploadResult.PermanentFailure(error.errorCode, error.message)
                } else {
                    UploadResult.TransientFailure(error.message)
                }
            }
        } catch (t: Throwable) {
            val updated = entry.copy(
                status = UploadStatus.FAILED,
                serverMessage = t.message ?: t.javaClass.simpleName,
                lastAttemptAtEpochMillis = attemptTime,
                attemptCount = entry.attemptCount + 1
            )
            dao.update(updated)
            UploadResult.TransientFailure(t.message)
        }
    }

    companion object {
        /** ISO-8601 with numeric UTC offset, e.g. "2026-11-14T09:45:03+06:00", matching API_CONTRACT.md §2. */
        fun isoFormat(epochMillis: Long): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
            sdf.timeZone = TimeZone.getDefault()
            return sdf.format(Date(epochMillis))
        }
    }
}
