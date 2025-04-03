package com.example.struku.domain.repository

import com.example.struku.domain.model.LineItem
import com.example.struku.domain.model.Receipt
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Repository interface for managing receipt data
 */
interface ReceiptRepository {
    /**
     * Get all receipts
     */
    fun getReceipts(): Flow<List<Receipt>>
    
    /**
     * Get a receipt by ID
     */
    suspend fun getReceipt(id: Long): Receipt?
    
    /**
     * Insert a new receipt
     * @return ID of the inserted receipt
     */
    suspend fun insertReceipt(receipt: Receipt): Long
    
    /**
     * Update an existing receipt
     */
    suspend fun updateReceipt(receipt: Receipt)
    
    /**
     * Delete a receipt
     */
    suspend fun deleteReceipt(receipt: Receipt)
    
    /**
     * Get all line items for a receipt
     */
    suspend fun getLineItems(receiptId: Long): List<LineItem>
    
    /**
     * Insert a new line item
     */
    suspend fun insertLineItem(lineItem: LineItem): Long
    
    /**
     * Update an existing line item
     */
    suspend fun updateLineItem(lineItem: LineItem)
    
    /**
     * Delete a line item
     */
    suspend fun deleteLineItem(lineItem: LineItem)
    
    /**
     * Get receipts by category
     */
    fun getReceiptsByCategory(category: String): Flow<List<Receipt>>
    
    /**
     * Get receipts between two dates
     */
    fun getReceiptsByDateRange(startDate: Long, endDate: Long): Flow<List<Receipt>>
    
    /**
     * Get receipts from a merchant
     */
    fun getReceiptsByMerchant(merchantName: String): Flow<List<Receipt>>
    
    /**
     * Search receipts by query
     */
    fun searchReceipts(query: String): Flow<List<Receipt>>
    
    /**
     * Get total spending by category
     */
    fun getTotalByCategory(): Flow<Map<String, Double>>
    
    /**
     * Get total spending by category within date range
     */
    suspend fun getTotalByCategory(startDate: Date, endDate: Date): Map<String, Double>
    
    /**
     * Get total spending by month
     */
    fun getTotalByMonth(): Flow<Map<String, Double>>
}