package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.TtsAudioEngine
import com.example.data.AppDatabase
import com.example.data.PaymentRecordEntity
import com.example.data.UpiAppSelectionStore
import com.example.service.PayBoxServiceState
import com.example.service.PaymentNotificationParser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PayBoxViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val paymentDao = database.paymentDao()
    private var ttsEngine: TtsAudioEngine? = null

    private val auth = FirebaseAuth.getInstance()
    private val dbRef = FirebaseDatabase.getInstance("https://fir-projects-paybox-default-rtdb.firebaseio.com").reference

    private val _currentUser = MutableStateFlow(auth.currentUser)
    val currentUser: StateFlow<com.google.firebase.auth.FirebaseUser?> = _currentUser.asStateFlow()

    private val _isPaid = MutableStateFlow(true)
    val isPaid: StateFlow<Boolean> = _isPaid.asStateFlow()

    private val _expiryDate = MutableStateFlow(System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000))
    val expiryDate: StateFlow<Long> = _expiryDate.asStateFlow()

    private val _audioRouteMode = MutableStateFlow("BLUETOOTH_ONLY")
    val audioRouteMode: StateFlow<String> = _audioRouteMode.asStateFlow()

    val allPayments: StateFlow<List<PaymentRecordEntity>> = paymentDao.getAllPayments()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val isPayBoxRunning: StateFlow<Boolean> = PayBoxServiceState.isRunning
    val enableAllUpiApps: StateFlow<Boolean> = UpiAppSelectionStore.enableAll
    val selectedUpiAppIds: StateFlow<Set<String>> = UpiAppSelectionStore.selectedAppIds

    private val _bluetoothStatus = MutableStateFlow(
        TtsAudioEngine.BluetoothStatus(false, "Checking Bluetooth...")
    )
    val bluetoothStatus: StateFlow<TtsAudioEngine.BluetoothStatus> = _bluetoothStatus.asStateFlow()

    private val _warningMessage = MutableStateFlow<String?>(null)
    val warningMessage: StateFlow<String?> = _warningMessage.asStateFlow()

    init {
        PayBoxServiceState.initFromPrefs(application)
        UpiAppSelectionStore.init(application)
        initAudioEngine()
        checkAuthAndSubscription()
    }

    private fun initAudioEngine(): TtsAudioEngine {
        ttsEngine?.let { return it }
        val engine = TtsAudioEngine(getApplication()).apply {
            audioRouteMode = _audioRouteMode.value
            setOnBluetoothStatusChangedListener { updatedStatus ->
                _bluetoothStatus.value = updatedStatus
                if (updatedStatus.isConnected) {
                    _warningMessage.value = null
                }
            }
        }
        ttsEngine = engine
        _bluetoothStatus.value = engine.checkBluetoothSpeakerStatus()
        return engine
    }

    private fun releaseAudioEngine() {
        ttsEngine?.setOnBluetoothStatusChangedListener(null)
        ttsEngine?.shutdown()
        ttsEngine = null
    }

    fun setAudioRouteMode(mode: String) {
        _audioRouteMode.value = mode
        ttsEngine?.audioRouteMode = mode
        val prefs = getApplication<Application>().getSharedPreferences("paybox_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("audio_route_mode", mode).apply()
    }

    fun checkAuthAndSubscription() {
        val user = auth.currentUser
        _currentUser.value = user
        if (user != null) {
            syncSubscriptionFromFirebase(user.uid, user.email ?: "merchant@paybox.com")
        }
    }

    private fun syncSubscriptionFromFirebase(uid: String, email: String) {
        val userRef = dbRef.child("users").child(uid)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    // First time login: create 7-day free trial
                    val trialExpiry = System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000)
                    val userData = mapOf(
                        "email" to email,
                        "isPaid" to true,
                        "expiryDate" to trialExpiry,
                        "createdAt" to System.currentTimeMillis()
                    )
                    userRef.setValue(userData)
                    _isPaid.value = true
                    _expiryDate.value = trialExpiry
                    saveSubscriptionToPrefs(true, trialExpiry)
                } else {
                    val paid = snapshot.child("isPaid").getValue(Boolean::class.java) ?: true
                    val expiry = snapshot.child("expiryDate").getValue(Long::class.java) ?: (System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000))
                    _isPaid.value = paid
                    _expiryDate.value = expiry
                    saveSubscriptionToPrefs(paid, expiry)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // fallback to local prefs if offline
                loadSubscriptionFromPrefs()
            }
        })
    }

    private fun saveSubscriptionToPrefs(isPaid: Boolean, expiryDate: Long) {
        val prefs = getApplication<Application>().getSharedPreferences("paybox_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("sub_is_paid", isPaid)
            .putLong("sub_expiry", expiryDate)
            .apply()
    }

    private fun loadSubscriptionFromPrefs() {
        val prefs = getApplication<Application>().getSharedPreferences("paybox_prefs", Context.MODE_PRIVATE)
        _isPaid.value = prefs.getBoolean("sub_is_paid", true)
        _expiryDate.value = prefs.getLong("sub_expiry", System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000))
    }

    fun signInWithGoogle(context: Context, onError: (String) -> Unit) {
        // Quick demo / direct login or credential manager
        val dummyEmail = "merchant.google@paybox.in"
        val dummyUid = "google_user_" + System.currentTimeMillis()
        auth.signInAnonymously().addOnSuccessListener { res ->
            val user = res.user
            if (user != null) {
                _currentUser.value = user
                syncSubscriptionFromFirebase(user.uid, dummyEmail)
            }
        }.addOnFailureListener { e ->
            onError(e.message ?: "Google Sign-In failed")
        }
    }

    fun signInWithEmail(email: String, pass: String, onError: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { res ->
                val user = res.user
                _currentUser.value = user
                if (user != null) {
                    syncSubscriptionFromFirebase(user.uid, email)
                }
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Sign-in failed")
            }
    }

    fun signUpWithEmail(email: String, pass: String, onError: (String) -> Unit) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { res ->
                val user = res.user
                _currentUser.value = user
                if (user != null) {
                    syncSubscriptionFromFirebase(user.uid, email)
                }
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Sign-up failed")
            }
    }

    fun signOut() {
        auth.signOut()
        _currentUser.value = null
        stopPayBox()
    }

    fun paySubscription() {
        val user = auth.currentUser ?: return
        val newExpiry = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000) // 30 days paid subscription
        val updates = mapOf(
            "isPaid" to true,
            "expiryDate" to newExpiry,
            "lastPaidAt" to System.currentTimeMillis()
        )
        dbRef.child("users").child(user.uid).updateChildren(updates).addOnSuccessListener {
            _isPaid.value = true
            _expiryDate.value = newExpiry
            saveSubscriptionToPrefs(true, newExpiry)
        }
    }

    fun setEnableAllUpiApps(enabled: Boolean) {
        UpiAppSelectionStore.setEnableAll(getApplication(), enabled)
    }

    fun toggleUpiAppSelection(appId: String, enabled: Boolean) {
        UpiAppSelectionStore.setAppEnabled(getApplication(), appId, enabled)
    }

    fun startPayBox() {
        val now = System.currentTimeMillis()
        val expired = _expiryDate.value < now && !_isPaid.value
        if (expired) {
            _warningMessage.value = "Subscription expired! Please pay your pending amount to start Pay Box."
            initAudioEngine().announceText("Your payment is pending, please pay your pending amount.") { }
            return
        }

        initAudioEngine()
        PayBoxServiceState.startPayBox(getApplication())
        _warningMessage.value = null
    }

    fun stopPayBox() {
        PayBoxServiceState.stopPayBox(getApplication(), closeApp = false)
        releaseAudioEngine()
    }

    fun stopAndCloseApp() {
        releaseAudioEngine()
        PayBoxServiceState.stopPayBox(getApplication(), closeApp = true)
    }

    fun togglePayBox(enable: Boolean) {
        if (enable) startPayBox() else stopPayBox()
    }

    fun refreshBluetoothStatus() {
        val engine = initAudioEngine()
        _bluetoothStatus.value = engine.checkBluetoothSpeakerStatus()
    }

    fun dismissWarning() {
        _warningMessage.value = null
    }

    fun testAnnouncement() {
        val now = System.currentTimeMillis()
        val expired = _expiryDate.value < now && !_isPaid.value
        val engine = initAudioEngine()
        _bluetoothStatus.value = engine.checkBluetoothSpeakerStatus()

        if (expired) {
            _warningMessage.value = "Your payment is pending, please pay your pending amount."
            engine.announceText("Your payment is pending, please pay your pending amount.") { warning ->
                _warningMessage.value = warning
            }
        } else {
            engine.testAnnouncement { warning ->
                _warningMessage.value = warning
            }
        }
    }

    fun simulatePayment(amount: Double, sender: String, appName: String) {
        val now = System.currentTimeMillis()
        val expired = _expiryDate.value < now && !_isPaid.value
        val engine = initAudioEngine()
        _bluetoothStatus.value = engine.checkBluetoothSpeakerStatus()

        if (expired) {
            _warningMessage.value = "Payment locked: Subscription expired! Please pay rent."
            engine.announceText("Your payment is pending, please pay your pending amount.") { warning ->
                _warningMessage.value = warning
            }
            return
        }

        val formattedAmount = PaymentNotificationParser.formatAmount(amount)
        val spokenText = "S-PROJECTS Pay Box par $formattedAmount rupaye mile."

        viewModelScope.launch {
            paymentDao.insertPayment(
                PaymentRecordEntity(
                    amount = amount,
                    senderName = sender,
                    appName = appName,
                    timestamp = now,
                    spokenText = spokenText
                )
            )
        }

        engine.announcePayment(amount) { warning ->
            _warningMessage.value = warning
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            paymentDao.clearAll()
        }
    }

    override fun onCleared() {
        super.onCleared()
        releaseAudioEngine()
    }
}
