package com.example.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object PayBoxServiceState {
    private const val TAG = "PayBoxServiceState"
    private const val PREFS_NAME = "paybox_service_prefs"
    private const val KEY_SERVICE_ENABLED = "service_enabled"

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _closeAppEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val closeAppEvents: SharedFlow<Unit> = _closeAppEvents.asSharedFlow()

    @Volatile
    private var activeListenerInstance: PaymentNotificationListenerService? = null

    fun initFromPrefs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isRunning.value = prefs.getBoolean(KEY_SERVICE_ENABLED, false)
    }

    fun isServiceEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return _isRunning.value && prefs.getBoolean(KEY_SERVICE_ENABLED, false)
    }

    fun registerListener(service: PaymentNotificationListenerService) {
        activeListenerInstance = service
    }

    fun unregisterListener(service: PaymentNotificationListenerService) {
        if (activeListenerInstance === service) {
            activeListenerInstance = null
        }
    }

    fun isNotificationAccessGranted(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners != null && enabledListeners.contains(context.packageName)
    }

    fun startPayBox(context: Context) {
        val appContext = context.applicationContext
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SERVICE_ENABLED, true)
            .apply()
        _isRunning.value = true

        val intent = Intent(appContext, PayBoxForegroundService::class.java).apply {
            action = PayBoxForegroundService.ACTION_START_SERVICE
        }
        ContextCompat.startForegroundService(appContext, intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isNotificationAccessGranted(appContext)) {
            try {
                NotificationListenerService.requestRebind(
                    ComponentName(appContext, PaymentNotificationListenerService::class.java)
                )
            } catch (e: Exception) {
                Log.w(TAG, "requestRebind skipped: ${e.message}")
            }
        }
        Log.i(TAG, "PayBox service started.")
    }

    fun stopPayBox(context: Context, closeApp: Boolean = false) {
        val appContext = context.applicationContext
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SERVICE_ENABLED, false)
            .apply()
        _isRunning.value = false

        // Safely release active listener audio/TTS resources and unbind
        activeListenerInstance?.shutdownAndUnbind()

        // Stop the foreground service
        val stopIntent = Intent(appContext, PayBoxForegroundService::class.java).apply {
            action = PayBoxForegroundService.ACTION_STOP_SERVICE
        }
        try {
            appContext.stopService(stopIntent)
        } catch (e: Exception) {
            Log.w(TAG, "stopService warning: ${e.message}")
        }

        if (closeApp) {
            _closeAppEvents.tryEmit(Unit)
        }
        Log.i(TAG, "PayBox service stopped (closeApp=$closeApp).")
    }
}
