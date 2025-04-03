package com.example.struku.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.struku.data.local.entity.LineItemEntity
import com.example.struku.data.local.entity.ReceiptEntity
import com.example.struku.data.local.relation.ReceiptWithItems
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Receipt and LineItem entities
 */
@Dao
interface ReceiptDao {
    /**
     * Get all receipts with their line items
     */
    @Transaction
    @Query("SELECT * FROM receipts ORDER BY date DESC")
    fun getAllReceiptsWithItems(): Flow<List<ReceiptWithItems>>
    
    /**
     * Get a specific receipt with its line items
     */
    @Transaction
    @Query("SELECT * FROM receipts WHERE id = :receiptId")
    suspend fun getReceiptWithItems(receiptId: Long): ReceiptWithItems?
    
    /**
     * Insert a new receipt
     * @return the ID of the inserted receipt
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: ReceiptEntity): Long
    
    /**
     * Update an existing receipt
     */
    @Update
    suspend fun updateReceipt(receipt: ReceiptEntity)
    
    /**
     * Delete a receipt
     */
    @Delete
    suspend fun deleteReceipt(receipt: ReceiptEntity)
    
    /**
     * Get receipts by category
     */
    @Transaction
    @Query("SELECT * FROM receipts WHERE category = :category ORDER BY date DESC")
    fun getReceiptsByCategory(category: String): Flow<List<ReceiptWithItems>>
    
    /**
     * Get receipts within a date range
     */
    @Transaction
    @Query("SELECT * FROM receipts WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getReceiptsByDateRange(startDate: Long, endDate: Long): Flow<List<ReceiptWithItems>>
    
    /**
     * Get receipts by merchant name
     */
    @Transaction
    @Query("SELECT * FROM receipts WHERE merchantName LIKE :merchantName ORDER BY date DESC")
    fun getReceiptsByMerchant(merchantName: String): Flow<List<ReceiptWithItems>>
    
    /**
     * Search receipts by merchant name, category, or notes
     */
    @Transaction
    @Query("SELECT * FROM receipts WHERE merchantName LIKE :query OR category LIKE :query OR notes LIKE :query ORDER BY date DESC")
    fun searchReceipts(query: String): Flow<List<ReceiptWithItems>>
    
    /**
     * Get total spending by category
     */
    @Query("SELECT category as categoryName, SUM(total) as categoryTotal FROM receipts GROUP BY category")
    fun getTotalByCategory(): Flow<List<CategoryTotal>>
    
    /**
     * Get total spending by category within date range
     */
    @Query("SELECT category as categoryName, SUM(total) as categoryTotal FROM receipts WHERE date BETWEEN :startDate AND :endDate GROUP BY category")
    suspend fun getTotalByCategoryInDateRange(startDate: Long, endDate: Long): List<CategoryTotal>
    
    /**
     * Get total spending by month
     */
    @Query("SELECT strftime('%Y-%m', datetime(date/1000, 'unixepoch')) as monthName, SUM(total) as monthTotal FROM receipts GROUP BY monthName ORDER BY monthName")
    fun getTotalByMonth(): Flow<List<MonthTotal>>
    
    /**
     * Insert a new line item
     * @return the ID of the inserted line item
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineItem(lineItem: LineItemEntity): Long
    
    /**
     * Update an existing line item
     */
    @Update
    suspend fun updateLineItem(lineItem: LineItemEntity)
    
    /**
     * Delete a line item
     */
    @Delete
    suspend fun deleteLineItem(lineItem: LineItemEntity)
    
    /**
     * Delete all line items for a receipt
     */
    @Query("DELETE FROM line_items WHERE receiptId = :receiptId")
    suspend fun deleteLineItemsByReceiptId(receiptId: Long)
    
    /**
     * Get all line items for a receipt
     */
    @Query("SELECT * FROM line_items WHERE receiptId = :receiptId")
    suspend fun getLineItemsForReceipt(receiptId: Long): List<LineItemEntity>
}

/**
 * Data class for category totals
 */
data class CategoryTotal(
    val categoryName: String,
    val categoryTotal: Double
)

/**
 * Data class for month totals
 */
data class MonthTotal(
    val monthName: String,
    val monthTotal: Double
)
