package com.example.data

import kotlinx.coroutines.flow.Flow

class TaxiRepository(private val taxiDao: TaxiDao) {
    val allTransactions: Flow<List<TaxiTransaction>> = taxiDao.getAllTransactions()

    suspend fun insert(transaction: TaxiTransaction): Long {
        return taxiDao.insertTransaction(transaction)
    }

    suspend fun delete(transaction: TaxiTransaction) {
        taxiDao.deleteTransaction(transaction)
    }

    suspend fun update(transaction: TaxiTransaction) {
        taxiDao.updateTransaction(transaction)
    }

    suspend fun getById(id: Long): TaxiTransaction? {
        return taxiDao.getTransactionById(id)
    }
}
