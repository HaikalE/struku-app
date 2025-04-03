package com.example.struku.data.mapper

import com.example.struku.data.local.entity.LineItemEntity
import com.example.struku.domain.model.LineItem

/**
 * Maps domain model to entity
 */
fun LineItem.toLineItemEntity(): LineItemEntity {
    return LineItemEntity(
        id = id,
        receiptId = 0, // This needs to be set by the caller
        name = name,
        price = price,
        quantity = quantity,
        category = category,
        notes = notes
    )
}

/**
 * Maps entity to domain model
 */
fun LineItemEntity.toLineItem(): LineItem {
    return LineItem(
        id = id,
        name = name,
        price = price,
        quantity = quantity,
        category = category,
        notes = notes ?: ""
    )
}
