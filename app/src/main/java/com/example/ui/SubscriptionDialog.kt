package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SubscriptionDialog(
    isPaid: Boolean,
    expiryDate: Long,
    isTrial: Boolean,
    onPaySubscription: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val formattedExpiry = dateFormat.format(Date(expiryDate))
    val now = System.currentTimeMillis()
    val isExpired = expiryDate < now && !isPaid
    val daysLeft = maxOf(0L, (expiryDate - now) / (1000 * 60 * 60 * 24))

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isExpired) Icons.Default.LockClock else Icons.Default.Verified,
                    contentDescription = null,
                    tint = if (isExpired) ErrorRed else SuccessGreen
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isExpired) "Subscription Expired" else "Subscription & Rent Status",
                    color = if (isExpired) ErrorRed else NeonCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isExpired) {
                    Text(
                        text = "Your 7-day free trial has ended and payment is pending. UPI transaction processing is locked until rent subscription is paid.",
                        color = ErrorRed,
                        fontSize = 13.sp
                    )
                } else if (isTrial) {
                    Text(
                        text = "You are currently enjoying your 7-Day Free Trial. $daysLeft days remaining before renewal.",
                        color = WarningAmber,
                        fontSize = 13.sp
                    )
                } else {
                    Text(
                        text = "Your Paid Subscription is active and verified via Firebase Realtime Database.",
                        color = SuccessGreen,
                        fontSize = 13.sp
                    )
                }

                HorizontalDivider(color = TextSecondary.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Status:", color = TextSecondary, fontSize = 12.sp)
                    Text(
                        text = if (isExpired) "LOCKED (Expired)" else if (isTrial) "Free Trial ($daysLeft days)" else "Paid Subscription",
                        color = if (isExpired) ErrorRed else if (isTrial) WarningAmber else SuccessGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Valid Till:", color = TextSecondary, fontSize = 12.sp)
                    Text(text = formattedExpiry, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Razorpay Gateway:", color = TextSecondary, fontSize = 12.sp)
                    Text(text = "rzp_live_T3XuB57BqqsNzz", color = NeonCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pay monthly rent to unlock permanent sound box announcements & all 25+ UPI apps.",
                    color = TextSecondary.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onPaySubscription()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = DarkBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Pay Rent Subscription (₹499 / Month)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Close", color = TextSecondary)
            }
        }
    )
}
