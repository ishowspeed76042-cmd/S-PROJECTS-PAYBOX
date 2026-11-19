package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class PayBoxForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.i(TAG, "PayBoxForegroundService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                Log.i(TAG, "Stop Service action clicked from notification")
                PayBoxServiceState.stopPayBox(this, closeApp = false)
                stopForegroundAndSelf()
                return START_NOT_STICKY
            }
            ACTION_CLOSE_APP -> {
                Log.i(TAG, "Close App action clicked from notification")
                PayBoxServiceState.stopPayBox(this, closeApp = true)
                stopForegroundAndSelf()
                return START_NOT_STICKY
            }
            else -> {
                val notification = buildPersistentNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                return START_STICKY
            }
        }
    }

    private fun stopForegroundAndSelf() {
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            Log.w(TAG, "stopForeground warning: ${e.message}")
        }
        stopSelf()
    }

    private fun buildPersistentNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            REQ_OPEN_APP,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopServiceIntent = Intent(this, PayBoxForegroundService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopServicePendingIntent = PendingIntent.getService(
            this,
            REQ_STOP_SERVICE,
            stopServiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val closeAppIntent = Intent(this, PayBoxForegroundService::class.java).apply {
            action = ACTION_CLOSE_APP
        }
        val closeAppPendingIntent = PendingIntent.getService(
            this,
            REQ_CLOSE_APP,
            closeAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("S-PROJECTS Pay Box Active")
            .setContentText("Monitoring selected Indian UPI credits on Bluetooth audio")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop Service",
                stopServicePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Close App",
                closeAppPendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "S-PROJECTS Pay Box Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps verified UPI payment detection active with quick stop controls."
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // If user swiped the app away after disabling, or to prevent unexpected persistence when disabled:
        if (!PayBoxServiceState.isServiceEnabled(this)) {
            stopForegroundAndSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "PayBoxForegroundService destroyed cleanly")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "PayBoxFgService"
        private const val CHANNEL_ID = "paybox_service_channel"
        private const val NOTIFICATION_ID = 1001
        private const val REQ_OPEN_APP = 100
        private const val REQ_STOP_SERVICE = 101
        private const val REQ_CLOSE_APP = 102

        const val ACTION_START_SERVICE = "com.example.action.START_PAYBOX_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.action.STOP_PAYBOX_SERVICE"
        const val ACTION_CLOSE_APP = "com.example.action.CLOSE_PAYBOX_APP"
    }
}
