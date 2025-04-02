package com.example.struku.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.ColumnInfo
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
    
    // Define a data class for category totals
    data class CategoryTotal(
        @ColumnInfo(name = "category") val category: String,
        @ColumnInfo(name = "total") val total: Double
    )
    
    // Fixed query to return a list of data class objects instead of a Map
    @Query("SELECT category, SUM(total) as total FROM receipts WHERE date BETWEEN :startDate AND :endDate GROUP BY category")
    suspend fun getCategoryTotals(startDate: Date, endDate: Date): List<CategoryTotal>
    
    // Define a data class for monthly totals
    data class MonthlyTotal(
        @ColumnInfo(name = "yearMonth") val yearMonth: String,
        @ColumnInfo(name = "total") val total: Double
    )
    
    // Fixed query to return a list of data class objects instead of a Map
    @Query(
        "SELECT strftime('%Y-%m', date / 1000, 'unixepoch') as yearMonth, SUM(total) as total " +
        "FROM receipts " +
        "WHERE date BETWEEN :startDate AND :endDate " +
        "GROUP BY yearMonth"
    )
    suspend fun getMonthlyTotals(startDate: Date, endDate: Date): List<MonthlyTotal>
    
    // Extension function to convert CategoryTotal list to Map if needed
    fun List<CategoryTotal>.toMap(): Map<String, Double> {
        return this.associate { it.category to it.total }
    }
    
    // Extension function to convert MonthlyTotal list to Map if needed
    fun List<MonthlyTotal>.toMap(): Map<String, Double> {
        return this.associate { it.yearMonth to it.total }
    }
}