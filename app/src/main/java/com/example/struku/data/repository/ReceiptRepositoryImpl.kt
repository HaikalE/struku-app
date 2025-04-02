package com.example.struku.data.repository

import com.example.struku.domain.model.Receipt
import com.example.struku.domain.repository.ReceiptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementasi dari ReceiptRepository
 * 
 * Untuk saat ini, implementasi ini masih menggunakan data dummy
 * Kedepannya akan diganti dengan implementasi yang menggunakan Room Database
 */
@Singleton
class ReceiptRepositoryImpl @Inject constructor() : ReceiptRepository {
    
    // List untuk menyimpan struk sementara (sebagai pengganti database)
    private val receipts = mutableListOf<Receipt>()
    
    // Counter untuk ID
    private var idCounter = 1L
    
    override suspend fun saveReceipt(receipt: Receipt): Long {
        val id = idCounter++
        val newReceipt = receipt.copy(id = id)
        receipts.add(newReceipt)
        return id
    }
    
    override suspend fun updateReceipt(receipt: Receipt) {
        val index = receipts.indexOfFirst { it.id == receipt.id }
        if (index != -1) {
            receipts[index] = receipt.copy(updatedAt = Date())
        }
    }
    
    override suspend fun deleteReceipt(receiptId: Int) {
        receipts.removeIf { it.id.toInt() == receiptId }
    }
    
    override suspend fun getReceiptById(receiptId: Int): Receipt? {
        return receipts.find { it.id.toInt() == receiptId }
    }
    
    override fun getAllReceipts(): Flow<List<Receipt>> {
        return flowOf(receipts.sortedByDescending { it.date })
    }
    
    override fun getReceiptsByDateRange(startDate: Date, endDate: Date): Flow<List<Receipt>> {
        return flowOf(receipts.filter { 
            it.date.time >= startDate.time && it.date.time <= endDate.time 
        }.sortedByDescending { it.date })
    }
    
    override fun getReceiptsByCategory(category: String): Flow<List<Receipt>> {
        return flowOf(receipts.filter { 
            it.category.equals(category, ignoreCase = true) 
        }.sortedByDescending { it.date })
    }
    
    override suspend fun getTotalByCategory(startDate: Date, endDate: Date): Map<String, Double> {
        val filteredReceipts = receipts.filter { 
            it.date.time >= startDate.time && it.date.time <= endDate.time 
        }
        
        return filteredReceipts.groupBy { it.category }
            .mapValues { (_, receipts) -> receipts.sumOf { it.total } }
    }
    
    override suspend fun getTotalByMonth(startDate: Date, endDate: Date): Map<String, Double> {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
        val filteredReceipts = receipts.filter { 
            it.date.time >= startDate.time && it.date.time <= endDate.time 
        }
        
        return filteredReceipts.groupBy { dateFormat.format(it.date) }
            .mapValues { (_, receipts) -> receipts.sumOf { it.total } }
    }
}
