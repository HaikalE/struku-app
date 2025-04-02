package com.example.struku.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.struku.data.local.entity.LineItemEntity
import com.example.struku.data.local.entity.ReceiptEntity

data class ReceiptWithItems(
    @Embedded val receipt: ReceiptEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "receiptId"
    )
    val items: List<LineItemEntity>
)
