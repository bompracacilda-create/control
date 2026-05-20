package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaxiDao {
    @Query("SELECT * FROM taxi_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TaxiTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TaxiTransaction): Long

    @Delete
    suspend fun deleteTransaction(transaction: TaxiTransaction)

    @Update
    suspend fun updateTransaction(transaction: TaxiTransaction)

    @Query("SELECT * FROM taxi_transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TaxiTransaction?
}
