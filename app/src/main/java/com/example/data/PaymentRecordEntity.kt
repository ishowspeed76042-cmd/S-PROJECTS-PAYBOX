package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_records")
data class PaymentRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val amount: Double,
    val senderName: String,
    val appName: String,
    val timestamp: Long,
    val spokenText: String
)
