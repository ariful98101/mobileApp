package com.mfsrelay.app.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Enqueues (or re-enqueues) [SmsUploadWorker] jobs. */
object WorkScheduler {

    private const val INITIAL_BACKOFF_SECONDS = 30L

    private fun uniqueWorkName(entryId: String) = "sms_upload_$entryId"

    /**
     * Enqueues an upload for [entryId]. Uses [ExistingWorkPolicy.REPLACE] keyed on the entry id
     * so a manual "Retry" while a previous attempt is still backing off simply restarts that
     * one job instead of piling up duplicates.
     */
    fun enqueueUpload(context: Context, entryId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SmsUploadWorker>()
            .setInputData(Data.Builder().putString(SmsUploadWorker.KEY_ENTRY_ID, entryId).build())
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, INITIAL_BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(uniqueWorkName(entryId), ExistingWorkPolicy.REPLACE, request)
    }

    fun enqueueUploads(context: Context, entryIds: List<String>) {
        entryIds.forEach { enqueueUpload(context, it) }
    }
}
