package com.example.struku.domain.repository

import com.example.struku.domain.model.Receipt
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Repository interface untuk operasi terkait struk
 */
interface ReceiptRepository {
    /**
     * Menyimpan struk baru ke database
     * @return ID dari struk yang baru dibuat
     */
    suspend fun saveReceipt(receipt: Receipt): Long
    
    /**
     * Mengupdate struk yang sudah ada
     */
    suspend fun updateReceipt(receipt: Receipt)
    
    /**
     * Menghapus struk berdasarkan ID
     */
    suspend fun deleteReceipt(receiptId: Long)
    
    /**
     * Mendapatkan struk berdasarkan ID
     */
    suspend fun getReceiptById(receiptId: Long): Receipt?
    
    /**
     * Mendapatkan semua struk, diurutkan berdasarkan tanggal terbaru
     */
    fun getAllReceipts(): Flow<List<Receipt>>
    
    /**
     * Mendapatkan struk dalam rentang tanggal
     */
    fun getReceiptsByDateRange(startDate: Date, endDate: Date): Flow<List<Receipt>>
    
    /**
     * Mendapatkan struk berdasarkan kategori
     */
    fun getReceiptsByCategory(category: String): Flow<List<Receipt>>
    
    /**
     * Mendapatkan jumlah total pengeluaran per kategori dalam rentang tanggal
     * @return Map kategori ke jumlah total
     */
    suspend fun getTotalByCategory(startDate: Date, endDate: Date): Map<String, Double>
    
    /**
     * Mendapatkan jumlah total pengeluaran per bulan dalam rentang tanggal
     * @return Map bulan (format yyyy-MM) ke jumlah total
     */
    suspend fun getTotalByMonth(startDate: Date, endDate: Date): Map<String, Double>
}