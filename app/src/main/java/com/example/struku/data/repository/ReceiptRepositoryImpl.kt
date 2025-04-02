package com.example.struku.data.repository

import com.example.struku.data.local.dao.LineItemDao
import com.example.struku.data.local.dao.ReceiptDao
import com.example.struku.data.mapper.toLineItemEntity
import com.example.struku.data.mapper.toReceipt
import com.example.struku.data.mapper.toReceiptEntity
import com.example.struku.domain.model.Receipt
import com.example.struku.domain.repository.ReceiptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject

class ReceiptRepositoryImpl @Inject constructor(
    private val receiptDao: ReceiptDao,
    private val lineItemDao: LineItemDao
) : ReceiptRepository {

    override suspend fun saveReceipt(receipt: Receipt): Long {
        val receiptId = receiptDao.insertReceipt(receipt.toReceiptEntity())
        
        // Insert line items with the new receipt ID
        val lineItems = receipt.items.map { it.toLineItemEntity().copy(receiptId = receiptId.toInt()) }
        lineItemDao.insertLineItems(lineItems)
        
        return receiptId
    }

    override suspend fun updateReceipt(receipt: Receipt) {
        receiptDao.updateReceipt(receipt.toReceiptEntity())
        
        // Update line items: first delete existing ones, then insert new ones
        lineItemDao.deleteLineItemsForReceipt(receipt.id)
        val lineItems = receipt.items.map { it.toLineItemEntity() }
        lineItemDao.insertLineItems(lineItems)
    }

    override suspend fun deleteReceipt(receiptId: Int) {
        val receipt = receiptDao.getReceiptById(receiptId) ?: return
        receiptDao.deleteReceipt(receipt)
    }

    override suspend fun getReceiptById(receiptId: Int): Receipt? {
        return receiptDao.getReceiptWithItems(receiptId)?.toReceipt()
    }

    override fun getAllReceipts(): Flow<List<Receipt>> {
        return receiptDao.getAllReceipts().map { receiptWithItems ->
            receiptWithItems.map { it.toReceipt() }
        }
    }

    override fun getReceiptsByDateRange(startDate: Date, endDate: Date): Flow<List<Receipt>> {
        return receiptDao.getReceiptsByDateRange(startDate, endDate).map { receiptWithItems ->
            receiptWithItems.map { it.toReceipt() }
        }
    }

    override fun getReceiptsByCategory(category: String): Flow<List<Receipt>> {
        return receiptDao.getReceiptsByCategory(category).map { receiptWithItems ->
            receiptWithItems.map { it.toReceipt() }
        }
    }

    override suspend fun getTotalByCategory(startDate: Date, endDate: Date): Map<String, Double> {
        // Use the new getCategoryTotals method and convert the result to Map
        return receiptDao.getCategoryTotals(startDate, endDate).associate { 
            it.category to it.total 
        }
    }

    override suspend fun getTotalByMonth(startDate: Date, endDate: Date): Map<String, Double> {
        // Use the new getMonthlyTotals method and convert the result to Map
        return receiptDao.getMonthlyTotals(startDate, endDate).associate { 
            it.yearMonth to it.total 
        }
    }
}