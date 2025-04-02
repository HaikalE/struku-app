package com.example.struku.data.mapper

import com.example.struku.data.local.entity.ReceiptEntity
import com.example.struku.data.local.relation.ReceiptWithItems
import com.example.struku.domain.model.Receipt
import java.util.Date

/**
 * Maps domain model to entity
 */
fun Receipt.toReceiptEntity(): ReceiptEntity {
    return ReceiptEntity(
        id = id,
        merchantName = merchantName,
        date = date,
        total = total,
        currency = currency,
        category = category,
        imageUri = imageUri,
        notes = notes,
        createdAt = createdAt,
        updatedAt = Date() // Always update updatedAt when converting to entity
    )
}

/**
 * Maps entity with relation to domain model
 */
fun ReceiptWithItems.toReceipt(): Receipt {
    return Receipt(
        id = receipt.id,
        merchantName = receipt.merchantName,
        date = receipt.date,
        total = receipt.total,
        currency = receipt.currency,
        category = receipt.category,
        imageUri = receipt.imageUri,
        items = items.map { it.toLineItem() },
        notes = receipt.notes,
        createdAt = receipt.createdAt,
        updatedAt = receipt.updatedAt
    )
}
