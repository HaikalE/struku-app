package com.example.struku.data.mapper

import com.example.struku.data.local.entity.LineItemEntity
import com.example.struku.data.local.entity.ReceiptEntity
import com.example.struku.data.local.relation.ReceiptWithItems
import com.example.struku.domain.model.LineItem
import com.example.struku.domain.model.Receipt
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps domain models to entities and vice versa
 */
@Singleton
class ReceiptMapper @Inject constructor() {

    /**
     * Convert Receipt domain model to ReceiptEntity for storage
     */
    fun mapToEntity(receipt: Receipt): ReceiptEntity {
        return ReceiptEntity(
            id = receipt.id,
            merchantName = receipt.merchantName,
            date = receipt.date,
            total = receipt.total,
            currency = receipt.currency ?: "IDR",
            category = receipt.category,
            imageUri = receipt.imageUri,
            notes = receipt.notes,
            createdAt = receipt.createdAt ?: Date(),
            updatedAt = Date() // Always use current time for updates
        )
    }

    /**
     * Convert ReceiptEntity with its line items to Receipt domain model
     */
    fun mapToDomain(receiptWithItems: ReceiptWithItems): Receipt {
        return Receipt(
            id = receiptWithItems.receipt.id,
            merchantName = receiptWithItems.receipt.merchantName,
            date = receiptWithItems.receipt.date,
            total = receiptWithItems.receipt.total,
            currency = receiptWithItems.receipt.currency,
            category = receiptWithItems.receipt.category,
            imageUri = receiptWithItems.receipt.imageUri,
            items = receiptWithItems.items.map { mapLineItemToDomain(it) },
            notes = receiptWithItems.receipt.notes ?: "",
            createdAt = receiptWithItems.receipt.createdAt,
            updatedAt = receiptWithItems.receipt.updatedAt
        )
    }

    /**
     * Convert ReceiptEntity to Receipt domain model (without line items)
     */
    fun mapToDomain(receiptEntity: ReceiptEntity): Receipt {
        return Receipt(
            id = receiptEntity.id,
            merchantName = receiptEntity.merchantName,
            date = receiptEntity.date,
            total = receiptEntity.total,
            currency = receiptEntity.currency,
            category = receiptEntity.category,
            imageUri = receiptEntity.imageUri,
            items = emptyList(),
            notes = receiptEntity.notes ?: "",
            createdAt = receiptEntity.createdAt,
            updatedAt = receiptEntity.updatedAt
        )
    }

    /**
     * Convert LineItem domain model to LineItemEntity for storage
     */
    fun mapLineItemToEntity(lineItem: LineItem, receiptId: Long): LineItemEntity {
        return LineItemEntity(
            id = lineItem.id,
            receiptId = receiptId,
            name = lineItem.name,
            price = lineItem.price,
            quantity = lineItem.quantity,
            total = lineItem.price * lineItem.quantity,
            category = lineItem.category,
            notes = lineItem.notes
        )
    }

    /**
     * Convert LineItemEntity to LineItem domain model
     */
    fun mapLineItemToDomain(lineItemEntity: LineItemEntity): LineItem {
        return LineItem(
            id = lineItemEntity.id,
            name = lineItemEntity.name,
            price = lineItemEntity.price,
            quantity = lineItemEntity.quantity,
            category = lineItemEntity.category,
            notes = lineItemEntity.notes ?: ""
        )
    }
}
