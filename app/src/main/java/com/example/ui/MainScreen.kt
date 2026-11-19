package com.example.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PaymentRecordEntity
import com.example.data.UpiAppCatalog
import com.example.ui.theme.*
import com.example.viewmodel.PayBoxViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: PayBoxViewModel) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val isPaid by viewModel.isPaid.collectAsState()
    val expiryDate by viewModel.expiryDate.collectAsState()
    val audioRouteMode by viewModel.audioRouteMode.collectAsState()

    val payments by viewModel.allPayments.collectAsState()
    val bluetoothStatus by viewModel.bluetoothStatus.collectAsState()
    val warningMessage by viewModel.warningMessage.collectAsState()
    val isPayBoxRunning by viewModel.isPayBoxRunning.collectAsState()
    val enableAllUpiApps by viewModel.enableAllUpiApps.collectAsState()
    val selectedUpiAppIds by viewModel.selectedUpiAppIds.collectAsState()

    var showSimulateDialog by remember { mutableStateOf(false) }
    var showUpiPickerDialog by remember { mutableStateOf(false) }
    var showSubscriptionDialog by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val isExpired = expiryDate < now && !isPaid
    val isTrial = expiryDate > now && isPaid && ((expiryDate - now) / (1000 * 60 * 60 * 24)) <= 7
    val daysLeft = maxOf(0L, (expiryDate - now) / (1000 * 60 * 60 * 24))

    val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    val isNotificationAccessGranted = enabledListeners != null && enabledListeners.contains(context.packageName)

    if (currentUser == null) {
        LoginScreen(viewModel = viewModel)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isExpired -> ErrorRed
                                        !isPayBoxRunning -> WarningAmber
                                        bluetoothStatus.isConnected -> SuccessGreen
                                        else -> ErrorRed
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "S-PROJECTS PAY BOX",
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = if (isExpired) "Subscription Expired • Locked" else if (isPayBoxRunning) "Active • Multi-UPI" else "Stopped",
                                fontSize = 11.sp,
                                color = if (isExpired) ErrorRed else TextSecondary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSubscriptionDialog = true }) {
                        Icon(Icons.Default.CardMembership, contentDescription = "Subscription Status", tint = if (isExpired) ErrorRed else SuccessGreen)
                    }
                    IconButton(onClick = { viewModel.signOut() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out", tint = TextSecondary)
                    }
                    IconButton(onClick = { viewModel.refreshBluetoothStatus() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        floatingActionButton = {
            if (!isExpired) {
                FloatingActionButton(
                    onClick = { showSimulateDialog = true },
                    containerColor = NeonCyan,
                    contentColor = DarkBackground,
                    modifier = Modifier.testTag("simulate_payment_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Simulate Payment")
                }
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            warningMessage?.let { msg ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ErrorRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = msg,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.dismissWarning() }) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = TextSecondary)
                            }
                        }
                    }
                }
            }

            // 1. Subscription & Rent Management Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isExpired) ErrorRed else SuccessGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isExpired) Icons.Default.Lock else Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = if (isExpired) ErrorRed else SuccessGreen
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isExpired) "Subscription Expired" else if (isTrial) "7-Day Free Trial Active" else "Paid Subscription Active",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isExpired) ErrorRed else TextPrimary
                                    )
                                    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                    Text(
                                        text = if (isExpired) "Payment pending. Please pay rent to unlock." else "Valid until: ${dateFormat.format(Date(expiryDate))}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Button(
                                onClick = { showSubscriptionDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isExpired) ErrorRed else SuccessGreen,
                                    contentColor = DarkBackground
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(if (isExpired) "Pay Rent" else "Manage", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 2. Dual Audio Output Mode Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speaker, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Audio Output Mode", fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.setAudioRouteMode("BLUETOOTH_ONLY") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (audioRouteMode == "BLUETOOTH_ONLY") NeonCyan else DarkSurfaceElevated,
                                    contentColor = if (audioRouteMode == "BLUETOOTH_ONLY") DarkBackground else TextSecondary
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Bluetooth Speaker Only", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { viewModel.setAudioRouteMode("PHONE_SPEAKER") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (audioRouteMode == "PHONE_SPEAKER") WarningAmber else DarkSurfaceElevated,
                                    contentColor = if (audioRouteMode == "PHONE_SPEAKER") DarkBackground else TextSecondary
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Phone/Tablet Speaker", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. Pay Box Service Control Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isPayBoxRunning) SuccessGreen.copy(alpha = 0.6f) else TextSecondary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (isPayBoxRunning) Icons.Default.PowerSettingsNew else Icons.Default.PowerOff,
                                    contentDescription = null,
                                    tint = if (isPayBoxRunning) SuccessGreen else TextSecondary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Pay Box Service", fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(
                                        text = if (isExpired) "Locked due to expired subscription" else if (isPayBoxRunning) "Running • Monitoring UPI Apps" else "Stopped",
                                        fontSize = 11.sp,
                                        color = if (isExpired) ErrorRed else TextSecondary
                                    )
                                }
                            }
                            Switch(
                                checked = isPayBoxRunning && !isExpired,
                                onCheckedChange = { enabled -> if (!isExpired) viewModel.togglePayBox(enabled) else showSubscriptionDialog = true },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = DarkBackground,
                                    checkedTrackColor = SuccessGreen,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = DarkSurfaceElevated
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (isExpired) {
                                        showSubscriptionDialog = true
                                    } else {
                                        viewModel.togglePayBox(!isPayBoxRunning)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isExpired) ErrorRed else if (isPayBoxRunning) WarningAmber else SuccessGreen,
                                    contentColor = DarkBackground
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isExpired) Icons.Default.Lock else if (isPayBoxRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isExpired) "Pay Rent to Unlock" else if (isPayBoxRunning) "Stop Service" else "Start Service",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = { viewModel.stopAndCloseApp() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                                border = BorderStroke(1.dp, ErrorRed),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stop & Close", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 4. Multi-UPI Apps Selection Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = NeonCyan)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Monitored UPI Apps", fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(
                                        text = if (enableAllUpiApps) "All ${UpiAppCatalog.ALL_APPS.size} verified UPI apps enabled" else "${selectedUpiAppIds.size} of ${UpiAppCatalog.ALL_APPS.size} apps selected",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Switch(
                                checked = enableAllUpiApps,
                                onCheckedChange = { viewModel.setEnableAllUpiApps(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = DarkBackground,
                                    checkedTrackColor = NeonCyan,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = DarkSurfaceElevated
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showUpiPickerDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                            border = BorderStroke(1.dp, NeonCyan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Choose Specific UPI Apps", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // 5. Bluetooth Speaker Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (bluetoothStatus.isConnected) NeonCyan.copy(alpha = 0.5f) else ErrorRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (bluetoothStatus.isConnected) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
                                    contentDescription = null,
                                    tint = if (bluetoothStatus.isConnected) NeonCyan else ErrorRed
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Bluetooth Speaker", fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Badge(
                                containerColor = if (bluetoothStatus.isConnected) SuccessGreen.copy(alpha = 0.2f) else ErrorRed.copy(alpha = 0.2f),
                                contentColor = if (bluetoothStatus.isConnected) SuccessGreen else ErrorRed
                            ) {
                                Text(
                                    text = if (bluetoothStatus.isConnected) "CONNECTED" else "DISCONNECTED",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (bluetoothStatus.isConnected) "Device: ${bluetoothStatus.deviceName}" else "No Bluetooth Speaker Connected",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                                border = BorderStroke(1.dp, NeonCyan),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("BT Settings", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { viewModel.testAnnouncement() },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBackground),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Sound", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 6. Notification Listener Guard
            if (!isNotificationAccessGranted) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, WarningAmber),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = WarningAmber)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Notification Access Required", fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "Grant Notification Access so Pay Box can verify payment alerts.", fontSize = 12.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                                colors = ButtonDefaults.buttonColors(containerColor = WarningAmber, contentColor = DarkBackground),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Grant Notification Access", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 7. Recent Payments Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Verified Payment Logs (${payments.size})",
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontSize = 16.sp
                    )
                    if (payments.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearHistory() }) {
                            Text("Clear All", color = ErrorRed, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (payments.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No verified payments received yet.", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                items(payments) { record ->
                    PaymentCard(record = record)
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showUpiPickerDialog) {
        UpiSelectionDialog(
            enableAll = enableAllUpiApps,
            selectedAppIds = selectedUpiAppIds,
            onToggleEnableAll = { viewModel.setEnableAllUpiApps(it) },
            onToggleApp = { id, checked -> viewModel.toggleUpiAppSelection(id, checked) },
            onDismiss = { showUpiPickerDialog = false }
        )
    }

    if (showSimulateDialog) {
        SimulatePaymentDialog(
            onDismiss = { showSimulateDialog = false },
            onSimulate = { amount, sender, appName ->
                viewModel.simulatePayment(amount, sender, appName)
                showSimulateDialog = false
            }
        )
    }

    if (showSubscriptionDialog) {
        SubscriptionDialog(
            isPaid = isPaid,
            expiryDate = expiryDate,
            isTrial = isTrial,
            onPaySubscription = {
                viewModel.paySubscription()
            },
            onDismiss = { showSubscriptionDialog = false }
        )
    }
}

@Composable
fun PaymentCard(record: PaymentRecordEntity) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(record.timestamp))

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(containerColor = NeonBlue.copy(alpha = 0.2f), contentColor = NeonCyan) {
                        Text(
                            text = record.appName,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(record.senderName, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(record.spokenText, fontSize = 12.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(formattedDate, fontSize = 10.sp, color = TextSecondary.copy(alpha = 0.7f))
            }

            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "₹${if (record.amount % 1.0 == 0.0) record.amount.toInt() else record.amount}",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = SuccessGreen
            )
        }
    }
}

@Composable
fun SimulatePaymentDialog(
    onDismiss: () -> Unit,
    onSimulate: (Double, String, String) -> Unit
) {
    var amountText by remember { mutableStateOf("100") }
    var senderText by remember { mutableStateOf("Rahul Sharma") }
    var selectedApp by remember { mutableStateOf("PhonePe") }

    val apps = listOf("PhonePe", "Google Pay (GPay)", "Paytm", "FamPay / FamX", "Navi", "BHIM UPI", "CRED")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text("Simulate UPI Payment", color = NeonCyan, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = TextSecondary,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = senderText,
                    onValueChange = { senderText = it },
                    label = { Text("Sender Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = TextSecondary,
                        focusedLabelColor = NeonCyan,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Payment App", color = TextSecondary, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    apps.forEach { app ->
                        FilterChip(
                            selected = selectedApp == app,
                            onClick = { selectedApp = app },
                            label = { Text(app, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = DarkBackground,
                                containerColor = DarkSurfaceElevated,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 50.0
                    onSimulate(amt, senderText, selectedApp)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBackground)
            ) {
                Text("Announce & Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
