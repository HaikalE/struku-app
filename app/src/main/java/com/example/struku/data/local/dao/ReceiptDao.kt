package com.example.struku.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.struku.data.local.entity.ReceiptEntity
import com.example.struku.data.local.relation.ReceiptWithItems
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface ReceiptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: ReceiptEntity): Long
    
    @Update
    suspend fun updateReceipt(receipt: ReceiptEntity)
    
    @Delete
    suspend fun deleteReceipt(receipt: ReceiptEntity)
    
    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getReceiptById(id: Int): ReceiptEntity?
    
    @Transaction
    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getReceiptWithItems(id: Int): ReceiptWithItems?
    
    @Transaction
    @Query("SELECT * FROM receipts ORDER BY date DESC")
    fun getAllReceipts(): Flow<List<ReceiptWithItems>>
    
    @Transaction
    @Query("SELECT * FROM receipts WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getReceiptsByDateRange(startDate: Date, endDate: Date): Flow<List<ReceiptWithItems>>
    
    @Transaction
    @Query("SELECT * FROM receipts WHERE category = :category ORDER BY date DESC")
    fun getReceiptsByCategory(category: String): Flow<List<ReceiptWithItems>>
    
    @Query("SELECT category, SUM(total) as total FROM receipts WHERE date BETWEEN :startDate AND :endDate GROUP BY category")
    suspend fun getTotalByCategory(startDate: Date, endDate: Date): Map<String, Double>
    
    @Query(
        "SELECT strftime('%Y-%m', date / 1000, 'unixepoch') as yearMonth, SUM(total) as total " +
        "FROM receipts " +
        "WHERE date BETWEEN :startDate AND :endDate " +
        "GROUP BY yearMonth"
    )
    suspend fun getTotalByMonth(startDate: Date, endDate: Date): Map<String, Double>
}
