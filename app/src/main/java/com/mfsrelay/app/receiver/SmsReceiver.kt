package com.mfsrelay.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.mfsrelay.app.MfsRelayApplication
import com.mfsrelay.app.data.local.SmsLogEntry
import com.mfsrelay.app.data.local.UploadStatus
import com.mfsrelay.app.work.WorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Listens for incoming SMS. For every message part whose sender matches one of the enabled
 * methods' sender patterns (see Settings), parses it, inserts a PENDING_UPLOAD row into Room,
 * and enqueues a [com.mfsrelay.app.work.SmsUploadWorker] to upload it.
 *
 * Never calls [android.content.BroadcastReceiver.setResultData] / aborts the broadcast - this
 * app only *observes* SMS, it must never interfere with the user's normal default SMS app.
 */
class SmsReceiver : BroadcastReceiver() {

    // Receivers must return quickly; do the actual parse+DB+enqueue work on a short-lived scope.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return

        val sender = messages[0].originatingAddress ?: ""
        val fullBody = messages.joinToString(separator = "") { it.messageBody ?: "" }
        val receivedAt = System.currentTimeMillis()

        val pendingResult = goAsync()
        val app = context.applicationContext as MfsRelayApplication

        scope.launch {
            try {
                val classification = app.parserRegistry.classify(sender, fullBody)
                if (classification != null) {
                    val entry = SmsLogEntry(
                        id = UUID.randomUUID().toString(),
                        method = classification.parsed.method,
                        sender = sender,
                        customerPhone = classification.parsed.customerPhone,
                        trxId = classification.parsed.trxId,
                        amount = classification.parsed.amount,
                        rawMessage = fullBody,
                        receivedAtEpochMillis = receivedAt,
                        status = UploadStatus.PENDING_UPLOAD
                    )
                    app.repository.insert(entry)
                    WorkScheduler.enqueueUpload(context, entry.id)
                } else {
                    Log.d(TAG, "Ignored SMS from '$sender' - no enabled method pattern matched it")
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to process incoming SMS", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "SmsReceiver"
    }
}
