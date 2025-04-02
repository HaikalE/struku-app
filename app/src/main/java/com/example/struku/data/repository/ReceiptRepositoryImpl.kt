package com.example.struku.data.repository

import android.util.Log
import com.example.struku.data.local.dao.LineItemDao
import com.example.struku.data.local.dao.ReceiptDao
import com.example.struku.data.mapper.toLineItemEntity
import com.example.struku.data.mapper.toReceipt
import com.example.struku.data.mapper.toReceiptEntity
import com.example.struku.domain.model.Receipt
import com.example.struku.domain.repository.ReceiptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject

class ReceiptRepositoryImpl @Inject constructor(
    private val receiptDao: ReceiptDao,
    private val lineItemDao: LineItemDao
) : ReceiptRepository {

    private val TAG = "ReceiptRepositoryImpl"

    override suspend fun saveReceipt(receipt: Receipt): Long {
        try {
            Log.d(TAG, "Saving receipt: $receipt")
            val receiptId = receiptDao.insertReceipt(receipt.toReceiptEntity())
            
            // Insert line items with the new receipt ID
            val lineItems = receipt.items.map { it.toLineItemEntity().copy(receiptId = receiptId.toInt()) }
            Log.d(TAG, "Inserting ${lineItems.size} line items for receipt ID: $receiptId")
            lineItemDao.insertLineItems(lineItems)
            
            Log.d(TAG, "Receipt saved successfully with ID: $receiptId")
            return receiptId
        } catch (e: Exception) {
            Log.e(TAG, "Error saving receipt: ${e.message}", e)
            throw e
        }
    }

    override suspend fun updateReceipt(receipt: Receipt) {
        try {
            Log.d(TAG, "Updating receipt: ID=${receipt.id}, Items=${receipt.items.size}")
            receiptDao.updateReceipt(receipt.toReceiptEntity())
            
            // Update line items: first delete existing ones, then insert new ones
            Log.d(TAG, "Deleting existing line items for receipt ID: ${receipt.id}")
            lineItemDao.deleteLineItemsForReceipt(receipt.id)
            
            val lineItems = receipt.items.map { it.toLineItemEntity() }
            Log.d(TAG, "Inserting ${lineItems.size} line items")
            lineItemDao.insertLineItems(lineItems)
            
            Log.d(TAG, "Receipt updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating receipt: ${e.message}", e)
            throw e
        }
    }

    override suspend fun deleteReceipt(receiptId: Int) {
        try {
            Log.d(TAG, "Deleting receipt ID: $receiptId")
            val receipt = receiptDao.getReceiptById(receiptId) ?: run {
                Log.w(TAG, "Receipt not found for deletion: $receiptId")
                return
            }
            receiptDao.deleteReceipt(receipt)
            Log.d(TAG, "Receipt deleted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting receipt: ${e.message}", e)
            throw e
        }
    }

    override suspend fun getReceiptById(receiptId: Int): Receipt? {
        return try {
            Log.d(TAG, "Getting receipt by ID: $receiptId")
            val receipt = receiptDao.getReceiptWithItems(receiptId)?.toReceipt()
            if (receipt != null) {
                Log.d(TAG, "Receipt found: ${receipt.merchantName}, Items: ${receipt.items.size}")
            } else {
                Log.d(TAG, "Receipt not found with ID: $receiptId")
            }
            receipt
        } catch (e: Exception) {
            Log.e(TAG, "Error getting receipt by ID: ${e.message}", e)
            null
        }
    }

    override fun getAllReceipts(): Flow<List<Receipt>> {
        Log.d(TAG, "Getting all receipts")
        return receiptDao.getAllReceipts()
            .catch { e -> 
                Log.e(TAG, "Error in getAllReceipts flow: ${e.message}", e)
                throw e
            }
            .map { receiptWithItems ->
                Log.d(TAG, "Mapped ${receiptWithItems.size} receipts")
                receiptWithItems.map { it.toReceipt() }
            }
    }

    override fun getReceiptsByDateRange(startDate: Date, endDate: Date): Flow<List<Receipt>> {
        Log.d(TAG, "Getting receipts by date range: $startDate to $endDate")
        return receiptDao.getReceiptsByDateRange(startDate, endDate)
            .catch { e -> 
                Log.e(TAG, "Error in getReceiptsByDateRange flow: ${e.message}", e)
                throw e
            }
            .map { receiptWithItems ->
                Log.d(TAG, "Found ${receiptWithItems.size} receipts in date range")
                receiptWithItems.map { it.toReceipt() }
            }
    }

    override fun getReceiptsByCategory(category: String): Flow<List<Receipt>> {
        Log.d(TAG, "Getting receipts by category: $category")
        return receiptDao.getReceiptsByCategory(category)
            .catch { e -> 
                Log.e(TAG, "Error in getReceiptsByCategory flow: ${e.message}", e)
                throw e
            }
            .map { receiptWithItems ->
                Log.d(TAG, "Found ${receiptWithItems.size} receipts in category")
                receiptWithItems.map { it.toReceipt() }
            }
    }

    override suspend fun getTotalByCategory(startDate: Date, endDate: Date): Map<String, Double> {
        try {
            Log.d(TAG, "Getting totals by category from $startDate to $endDate")
            // Use the new getCategoryTotals method and convert the result to Map
            val result = receiptDao.getCategoryTotals(startDate, endDate).associate { 
                it.category to it.total 
            }
            Log.d(TAG, "Category totals: $result")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error getting totals by category: ${e.message}", e)
            throw e
        }
    }

    override suspend fun getTotalByMonth(startDate: Date, endDate: Date): Map<String, Double> {
        try {
            Log.d(TAG, "Getting totals by month from $startDate to $endDate")
            // Use the new getMonthlyTotals method and convert the result to Map
            val result = receiptDao.getMonthlyTotals(startDate, endDate).associate { 
                it.yearMonth to it.total 
            }
            Log.d(TAG, "Monthly totals: $result")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error getting totals by month: ${e.message}", e)
            throw e
        }
    }
}
