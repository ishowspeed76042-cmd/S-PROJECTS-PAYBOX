package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payment_records ORDER BY timestamp DESC")
    fun getAllPayments(): Flow<List<PaymentRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(record: PaymentRecordEntity)

    @Query("DELETE FROM payment_records")
    suspend fun clearAll()
}
