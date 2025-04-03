package com.example.struku.data.repository

import com.example.struku.data.local.dao.ReceiptDao
import com.example.struku.data.local.entity.LineItemEntity
import com.example.struku.data.mapper.ReceiptMapper
import com.example.struku.domain.model.LineItem
import com.example.struku.domain.model.Receipt
import com.example.struku.domain.repository.ReceiptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of the ReceiptRepository interface
 */
@Singleton
class ReceiptRepositoryImpl @Inject constructor(
    private val receiptDao: ReceiptDao,
    private val mapper: ReceiptMapper
) : ReceiptRepository {
    
    override fun getReceipts(): Flow<List<Receipt>> {
        return receiptDao.getAllReceiptsWithItems().map { list -> 
            list.map { mapper.mapToDomain(it) }
        }
    }
    
    override suspend fun getReceipt(id: Long): Receipt? {
        val receiptWithItems = receiptDao.getReceiptWithItems(id)
        return receiptWithItems?.let { mapper.mapToDomain(it) }
    }
    
    override suspend fun insertReceipt(receipt: Receipt): Long {
        val receiptEntity = mapper.mapToEntity(receipt)
        val receiptId = receiptDao.insertReceipt(receiptEntity)
        
        // Insert all line items with the new receipt ID
        receipt.items.forEach { item ->
            val itemEntity = mapper.mapLineItemToEntity(item, receiptId)
            receiptDao.insertLineItem(itemEntity)
        }
        
        return receiptId
    }
    
    override suspend fun updateReceipt(receipt: Receipt) {
        val receiptEntity = mapper.mapToEntity(receipt)
        receiptDao.updateReceipt(receiptEntity)
        
        // Handle line items - first delete existing items
        receiptDao.deleteLineItemsByReceiptId(receipt.id)
        
        // Then insert the updated items
        receipt.items.forEach { item ->
            val itemEntity = mapper.mapLineItemToEntity(item, receipt.id)
            receiptDao.insertLineItem(itemEntity)
        }
    }
    
    override suspend fun deleteReceipt(receipt: Receipt) {
        val receiptEntity = mapper.mapToEntity(receipt)
        receiptDao.deleteReceipt(receiptEntity)
        // Line items will be deleted by Room's cascading delete
    }
    
    override suspend fun getLineItems(receiptId: Long): List<LineItem> {
        return receiptDao.getLineItemsForReceipt(receiptId).map { 
            mapper.mapLineItemToDomain(it) 
        }
    }
    
    override suspend fun insertLineItem(lineItem: LineItem): Long {
        val lineItemEntity = mapper.mapLineItemToEntity(lineItem, lineItem.id)
        return receiptDao.insertLineItem(lineItemEntity)
    }
    
    override suspend fun updateLineItem(lineItem: LineItem) {
        val lineItemEntity = mapper.mapLineItemToEntity(lineItem, lineItem.id)
        receiptDao.updateLineItem(lineItemEntity)
    }
    
    override suspend fun deleteLineItem(lineItem: LineItem) {
        val lineItemEntity = LineItemEntity(
            id = lineItem.id,
            receiptId = 0, // This will be ignored during deletion
            name = lineItem.name,
            price = lineItem.price,
            quantity = lineItem.quantity,
            total = lineItem.price * lineItem.quantity,
            category = lineItem.category,
            notes = lineItem.notes
        )
        receiptDao.deleteLineItem(lineItemEntity)
    }
    
    override fun getReceiptsByCategory(category: String): Flow<List<Receipt>> {
        return receiptDao.getReceiptsByCategory(category).map { list ->
            list.map { mapper.mapToDomain(it) }
        }
    }
    
    override fun getReceiptsByDateRange(startDate: Long, endDate: Long): Flow<List<Receipt>> {
        return receiptDao.getReceiptsByDateRange(startDate, endDate).map { list ->
            list.map { mapper.mapToDomain(it) }
        }
    }
    
    override fun getReceiptsByMerchant(merchantName: String): Flow<List<Receipt>> {
        return receiptDao.getReceiptsByMerchant(merchantName).map { list ->
            list.map { mapper.mapToDomain(it) }
        }
    }
    
    override fun searchReceipts(query: String): Flow<List<Receipt>> {
        return receiptDao.searchReceipts("%$query%").map { list ->
            list.map { mapper.mapToDomain(it) }
        }
    }
    
    override fun getTotalByCategory(): Flow<Map<String, Double>> {
        return receiptDao.getTotalByCategory().map { categoryTotals ->
            categoryTotals.associate { it.categoryName to it.categoryTotal }
        }
    }
    
    override fun getTotalByMonth(): Flow<Map<String, Double>> {
        return receiptDao.getTotalByMonth().map { monthTotals ->
            monthTotals.associate { it.monthName to it.monthTotal }
        }
    }
}
