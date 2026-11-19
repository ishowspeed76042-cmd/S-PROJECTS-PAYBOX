package com.example.audio

import android.Manifest
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.service.PaymentNotificationParser
import java.util.Locale

class TtsAudioEngine(private val context: Context) {

    enum class BluetoothRouteType {
        NONE,
        A2DP,
        BLE,
        SCO
    }

    data class BluetoothStatus(
        val isConnected: Boolean,
        val deviceName: String,
        val routeType: BluetoothRouteType = BluetoothRouteType.NONE
    )

    private data class PendingAnnouncement(
        val text: String,
        val onWarning: (String) -> Unit
    )

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var tts: TextToSpeech? = null
    @Volatile
    private var isTtsInitialized = false
    private val pendingAnnouncements = mutableListOf<PendingAnnouncement>()
    private var onBluetoothStatusChanged: ((BluetoothStatus) -> Unit)? = null

    // "BLUETOOTH_ONLY" or "PHONE_SPEAKER"
    var audioRouteMode: String = "BLUETOOTH_ONLY"

    private var currentFocusRequest: AudioFocusRequest? = null
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            Log.w(TAG, "Audio focus lost permanently; stopping TTS.")
            stopSpeechSafely()
        }
    }

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            notifyStatusChanged()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            val status = checkBluetoothSpeakerStatus()
            if (!status.isConnected && audioRouteMode == "BLUETOOTH_ONLY") {
                Log.w(TAG, "Bluetooth audio output removed! Stopping TTS immediately.")
                stopSpeechSafely()
            }
            notifyStatusChanged()
        }
    }

    private val noisyAndBtReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    if (audioRouteMode == "BLUETOOTH_ONLY") {
                        Log.w(TAG, "ACTION_AUDIO_BECOMING_NOISY received — blocking internal speaker fallback!")
                        stopSpeechSafely()
                    }
                    notifyStatusChanged()
                }
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
                    if (state != BluetoothProfile.STATE_CONNECTED && audioRouteMode == "BLUETOOTH_ONLY") {
                        Log.w(TAG, "Bluetooth A2DP disconnected — halting TTS.")
                        stopSpeechSafely()
                    }
                    notifyStatusChanged()
                }
            }
        }
    }

    init {
        initTextToSpeech()
        registerAudioCallbacks()
    }

    fun setOnBluetoothStatusChangedListener(listener: ((BluetoothStatus) -> Unit)?) {
        onBluetoothStatusChanged = listener
    }

    private fun initTextToSpeech() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val langResult = tts?.setLanguage(Locale.forLanguageTag("en-IN"))
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.US)
                }

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        if (audioRouteMode == "BLUETOOTH_ONLY" && !checkBluetoothSpeakerStatus().isConnected) {
                            Log.e(TAG, "Bluetooth dropped at utterance start! Aborting playback.")
                            stopSpeechSafely()
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        abandonAudioFocus()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        abandonAudioFocus()
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        abandonAudioFocus()
                    }
                })

                isTtsInitialized = true
                flushPendingAnnouncements()
            } else {
                Log.e(TAG, "TTS initialization failed status=$status")
            }
        }
    }

    private fun registerAudioCallbacks() {
        try {
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, mainHandler)
            val filter = IntentFilter().apply {
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            }
            ContextCompat.registerReceiver(
                context.applicationContext,
                noisyAndBtReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register audio callbacks: ${e.message}", e)
        }
    }

    private fun notifyStatusChanged() {
        onBluetoothStatusChanged?.invoke(checkBluetoothSpeakerStatus())
    }

    private fun findConnectedBluetoothDevice(): Pair<AudioDeviceInfo?, BluetoothStatus> {
        val outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        for (device in outputDevices) {
            if (device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
                val name = device.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Bluetooth A2DP Speaker"
                return device to BluetoothStatus(true, name, BluetoothRouteType.A2DP)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            for (device in outputDevices) {
                if (device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                    device.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && device.type == AudioDeviceInfo.TYPE_BLE_BROADCAST)
                ) {
                    val name = device.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Bluetooth LE Speaker"
                    return device to BluetoothStatus(true, name, BluetoothRouteType.BLE)
                }
            }
        }

        for (device in outputDevices) {
            if (device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                val name = device.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Bluetooth SCO Device"
                return device to BluetoothStatus(true, name, BluetoothRouteType.SCO)
            }
        }

        if (isBluetoothA2dpProfileConnected()) {
            return null to BluetoothStatus(true, "Connected Bluetooth A2DP Speaker", BluetoothRouteType.A2DP)
        }

        return null to BluetoothStatus(false, "No Bluetooth Speaker", BluetoothRouteType.NONE)
    }

    private fun isBluetoothA2dpProfileConnected(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
            ) {
                false
            } else {
                val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val adapter: BluetoothAdapter? = btManager?.adapter
                adapter != null &&
                    adapter.isEnabled &&
                    adapter.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_CONNECTED
            }
        } catch (e: Exception) {
            false
        }
    }

    fun checkBluetoothSpeakerStatus(): BluetoothStatus {
        return findConnectedBluetoothDevice().second
    }

    fun announcePayment(amount: Double, onWarning: (String) -> Unit) {
        val formattedAmount = PaymentNotificationParser.formatAmount(amount)
        val text = "S-PROJECTS Pay Box par $formattedAmount rupaye mile."
        speakText(text, onWarning)
    }

    fun announceText(text: String, onWarning: (String) -> Unit) {
        speakText(text, onWarning)
    }

    private fun speakText(text: String, onWarning: (String) -> Unit) {
        val (_, btStatus) = findConnectedBluetoothDevice()

        if (audioRouteMode == "BLUETOOTH_ONLY" && !btStatus.isConnected) {
            val warningMsg = "BLUETOOTH ERROR: No Bluetooth speaker connected! Audio blocked."
            Log.e(TAG, warningMsg)
            onWarning(warningMsg)
            return
        }

        if (!isTtsInitialized) {
            synchronized(pendingAnnouncements) {
                pendingAnnouncements.add(PendingAnnouncement(text, onWarning))
            }
            return
        }

        speakInternal(text, btStatus, onWarning)
    }

    private fun flushPendingAnnouncements() {
        val toRun: List<PendingAnnouncement>
        synchronized(pendingAnnouncements) {
            toRun = pendingAnnouncements.toList()
            pendingAnnouncements.clear()
        }
        toRun.forEach { item ->
            val (_, btStatus) = findConnectedBluetoothDevice()
            speakInternal(item.text, btStatus, item.onWarning)
        }
    }

    private fun speakInternal(text: String, btStatus: BluetoothStatus, onWarning: (String) -> Unit) {
        if (audioRouteMode == "BLUETOOTH_ONLY" && !btStatus.isConnected) {
            onWarning("BLUETOOTH ERROR: Bluetooth speaker disconnected before playback.")
            return
        }

        val streamType = if (audioRouteMode == "BLUETOOTH_ONLY" && btStatus.isConnected) {
            configureStrictBluetoothRouting(btStatus.routeType)
            requestAudioFocus(btStatus.routeType)
            AudioManager.STREAM_MUSIC
        } else {
            audioManager.mode = AudioManager.MODE_NORMAL
            AudioManager.STREAM_MUSIC
        }

        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, streamType)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        tts?.speak(text, TextToSpeech.QUEUE_ADD, params, "PayBoxUtterance_${System.currentTimeMillis()}")
    }

    @Suppress("DEPRECATION")
    private fun configureStrictBluetoothRouting(routeType: BluetoothRouteType): Int {
        return try {
            audioManager.isSpeakerphoneOn = false
            if (audioManager.isBluetoothScoOn) {
                audioManager.isBluetoothScoOn = false
                audioManager.stopBluetoothSco()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.clearCommunicationDevice()
            }
            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.isBluetoothA2dpOn = true

            val mediaAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts?.setAudioAttributes(mediaAttributes)
            AudioManager.STREAM_MUSIC
        } catch (e: Exception) {
            AudioManager.STREAM_MUSIC
        }
    }

    private fun requestAudioFocus(routeType: BluetoothRouteType) {
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(attrs)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener, mainHandler)
                    .build()
                currentFocusRequest = request
                audioManager.requestAudioFocus(request)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                currentFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
                currentFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(audioFocusChangeListener)
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun stopSpeechSafely() {
        try {
            synchronized(pendingAnnouncements) { pendingAnnouncements.clear() }
            tts?.stop()
            abandonAudioFocus()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun testAnnouncement(onWarning: (String) -> Unit) {
        announcePayment(50.0, onWarning)
    }

    fun shutdown() {
        try {
            stopSpeechSafely()
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
            context.applicationContext.unregisterReceiver(noisyAndBtReceiver)
        } catch (e: Exception) {
            // ignore
        }
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            // ignore
        }
    }

    companion object {
        private const val TAG = "TtsAudioEngine"
    }
}
