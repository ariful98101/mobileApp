package com.mfsrelay.app

import android.app.Application
import androidx.work.Configuration
import com.mfsrelay.app.data.local.AppDatabase
import com.mfsrelay.app.data.remote.ApiClientProvider
import com.mfsrelay.app.data.repository.SmsLogRepository
import com.mfsrelay.app.parser.ParserRegistry
import com.mfsrelay.app.settings.SettingsManager

/**
 * Hand-rolled dependency container (no Hilt, to keep the build simple). Everything here is a
 * cheap lazily-built singleton scoped to the process; [SmsReceiver] and [SmsUploadWorker] read
 * them off this Application instance since they're created by the OS/WorkManager, not by us.
 */
class MfsRelayApplication : Application(), Configuration.Provider {

    val settingsManager: SettingsManager by lazy { SettingsManager(this) }

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val apiClientProvider: ApiClientProvider by lazy { ApiClientProvider(settingsManager) }

    val repository: SmsLogRepository by lazy {
        SmsLogRepository(database.smsLogDao(), apiClientProvider)
    }

    val parserRegistry: ParserRegistry by lazy { ParserRegistry(settingsManager) }

    override fun onCreate() {
        super.onCreate()
        // Nothing else to eagerly initialize - everything above is lazy and cheap.
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
