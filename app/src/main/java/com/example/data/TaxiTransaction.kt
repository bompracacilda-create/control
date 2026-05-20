package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "taxi_transactions")
data class TaxiTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val isRevenue: Boolean, // true for Receita (Revenue), false for Despesa (Expense)
    val amount: Double,
    val timestamp: Long, // Date and Time
    val category: String, // e.g., Combustível, Corrida App, Manutenção
    val route: String, // e.g., Centro -> Aeroporto
    val driverName: String, // e.g., João, Maria
    val notes: String = ""
)
