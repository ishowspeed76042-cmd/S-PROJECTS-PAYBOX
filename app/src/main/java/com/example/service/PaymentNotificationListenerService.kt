package com.example.service

import android.app.Notification
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.audio.TtsAudioEngine
import com.example.data.AppDatabase
import com.example.data.PaymentRecordEntity
import com.example.data.UpiAppSelectionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class PaymentNotificationListenerService : NotificationListenerService() {

    private var serviceScope: CoroutineScope? = null
    @Volatile
    private var ttsEngine: TtsAudioEngine? = null
    private val recentNotificationSignatures = ConcurrentHashMap<String, Long>()
    @Volatile
    private var isListenerConnected = false

    override fun onCreate() {
        super.onCreate()
        PayBoxServiceState.initFromPrefs(applicationContext)
        UpiAppSelectionStore.init(applicationContext)
        PayBoxServiceState.registerListener(this)
        Log.i(TAG, "PaymentNotificationListenerService created")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isListenerConnected = true
        if (!PayBoxServiceState.isServiceEnabled(applicationContext)) {
            Log.i(TAG, "PayBox is stopped; unbinding listener to avoid background persistence.")
            shutdownAndUnbind()
        } else {
            ensureResourcesInitialized()
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isListenerConnected = false
        releaseResources()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        if (!PayBoxServiceState.isServiceEnabled(applicationContext)) {
            return
        }

        val packageName = sbn.packageName ?: return
        if (packageName == applicationContext.packageName ||
            packageName == "android" ||
            packageName == "com.android.systemui"
        ) {
            return
        }

        val notification = sbn.notification ?: return
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0 ||
            (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        ) {
            return
        }

        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()
        val infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString().orEmpty()
        val senderEmailExtra = extractSenderMetadata(extras)

        val allowedAppIds = UpiAppSelectionStore.getEffectiveAllowedAppIds(applicationContext)

        val result = PaymentNotificationParser.parse(
            packageName = packageName,
            title = title,
            text = text,
            bigText = bigText,
            subText = subText,
            infoText = infoText,
            senderEmail = senderEmailExtra,
            allowedAppIds = allowedAppIds
        )

        if (!result.isValidCredit) {
            Log.d(
                TAG,
                "Blocked/Ignored notification from $packageName (${result.ignoreReason}): title='$title', text='$text'"
            )
            return
        }

        val signature = "${packageName}|${result.formattedAmount}|$title|$text"
        val now = System.currentTimeMillis()
        val previousTime = recentNotificationSignatures[signature]
        if (previousTime != null && (now - previousTime) < DEDUP_WINDOW_MS) {
            Log.d(TAG, "Skipping duplicate verified notification: $signature")
            return
        }
        recentNotificationSignatures[signature] = now
        cleanupOldSignatures(now)

        Log.i(
            TAG,
            "Verified credit detected! App=${result.appName}, Source=${result.verifiedSource}, Amount=₹${result.formattedAmount}"
        )

        ensureResourcesInitialized()

        val prefs = applicationContext.getSharedPreferences("paybox_prefs", android.content.Context.MODE_PRIVATE)
        val isPaid = prefs.getBoolean("sub_is_paid", true)
        val expiryDate = prefs.getLong("sub_expiry", System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000))
        val isExpired = expiryDate < now && !isPaid

        if (isExpired) {
            Log.w(TAG, "Subscription expired! Blocking payment processing and announcing pending payment.")
            ttsEngine?.announceText("Your payment is pending, please pay your pending amount.") { warning ->
                Log.w(TAG, warning)
            }
            return
        }

        serviceScope?.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                db.paymentDao().insertPayment(
                    PaymentRecordEntity(
                        amount = result.amount,
                        senderName = result.senderName,
                        appName = result.appName,
                        timestamp = now,
                        spokenText = result.spokenText
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save payment record: ${e.message}", e)
            }
        }

        ttsEngine?.announcePayment(result.amount) { warning ->
            Log.w(TAG, warning)
        }
    }

    private fun extractSenderMetadata(extras: android.os.Bundle): String {
        val candidates = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            @Suppress("DEPRECATION")
            val people = extras.getParcelableArrayList<android.app.Person>(Notification.EXTRA_PEOPLE_LIST)
            people?.forEach { person ->
                person.uri?.let { candidates.add(it) }
                person.name?.toString()?.let { candidates.add(it) }
            }
        }
        @Suppress("DEPRECATION")
        val legacyPeople = extras.getStringArray(Notification.EXTRA_PEOPLE)
        legacyPeople?.forEach { candidates.add(it) }
        return candidates.joinToString(" ")
    }

    private fun ensureResourcesInitialized() {
        if (serviceScope == null) {
            serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        }
        if (ttsEngine == null) {
            ttsEngine = TtsAudioEngine(applicationContext)
        }
    }

    private fun releaseResources() {
        try {
            ttsEngine?.shutdown()
        } catch (e: Exception) {
            Log.w(TAG, "Error shutting down TTS in listener: ${e.message}")
        }
        ttsEngine = null

        try {
            serviceScope?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling listener scope: ${e.message}")
        }
        serviceScope = null
        recentNotificationSignatures.clear()
    }

    fun shutdownAndUnbind() {
        releaseResources()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isListenerConnected) {
            try {
                requestUnbind()
                isListenerConnected = false
                Log.i(TAG, "NotificationListenerService unbound cleanly.")
            } catch (e: Exception) {
                Log.w(TAG, "requestUnbind failed: ${e.message}")
            }
        }
    }

    private fun cleanupOldSignatures(now: Long) {
        val iterator = recentNotificationSignatures.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if ((now - entry.value) > DEDUP_WINDOW_MS * 4) {
                iterator.remove()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PayBoxServiceState.unregisterListener(this)
        releaseResources()
        Log.i(TAG, "PaymentNotificationListenerService destroyed")
    }

    companion object {
        private const val TAG = "PaymentListener"
        private const val DEDUP_WINDOW_MS = 5000L
    }
}
