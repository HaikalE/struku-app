package com.example.struku.data.mapper

import com.example.struku.data.local.entity.LineItemEntity
import com.example.struku.domain.model.LineItem

/**
 * Maps domain model to entity
 */
fun LineItem.toLineItemEntity(): LineItemEntity {
    return LineItemEntity(
        id = id,
        receiptId = receiptId,
        description = description,
        quantity = quantity,
        unitPrice = unitPrice,
        price = price,
        category = category
    )
}

/**
 * Maps entity to domain model
 */
fun LineItemEntity.toLineItem(): LineItem {
    return LineItem(
        id = id,
        receiptId = receiptId,
        description = description,
        quantity = quantity,
        unitPrice = unitPrice,
        price = price,
        category = category
    )
}
