package com.mfsrelay.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mfsrelay.app.MfsRelayApplication
import com.mfsrelay.app.data.repository.UploadResult

/**
 * Uploads one [com.mfsrelay.app.data.local.SmsLogEntry] (identified by [KEY_ENTRY_ID]) to the
 * `/sms-webhook` endpoint. Enqueued once per captured SMS, and again (same entry id) by the
 * "Retry" / "Retry all failed" UI actions.
 *
 * Retry behaviour:
 * - Permanent failures (4xx - bad payload, bad/revoked key, etc.) return [Result.failure]: the
 *   row is left FAILED and WorkManager will not retry automatically. The user can retry manually
 *   once they've fixed the underlying problem (e.g. updated the API key).
 * - Transient failures (network error, timeout, 5xx) return [Result.retry]; combined with the
 *   exponential backoff policy set in [WorkScheduler], WorkManager keeps retrying on its own.
 */
class SmsUploadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val entryId = inputData.getString(KEY_ENTRY_ID) ?: return Result.failure()
        val repository = (applicationContext as MfsRelayApplication).repository

        return when (val outcome = repository.attemptUpload(entryId)) {
            is UploadResult.Success -> Result.success()
            is UploadResult.PermanentFailure -> Result.failure()
            is UploadResult.TransientFailure -> {
                if (runAttemptCount >= MAX_ATTEMPTS) Result.failure() else Result.retry()
            }
        }
    }

    companion object {
        const val KEY_ENTRY_ID = "entry_id"
        private const val MAX_ATTEMPTS = 8
    }
}
