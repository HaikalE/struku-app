package com.example.struku.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Database entity for line items
 */
@Entity(
    tableName = "line_items",
    foreignKeys = [
        ForeignKey(
            entity = ReceiptEntity::class,
            parentColumns = ["id"],
            childColumns = ["receiptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("receiptId")
    ]
)
data class LineItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val receiptId: Long,
    val name: String,
    val price: Double,
    val quantity: Double = 1.0,
    val total: Double = price * quantity,
    val category: String = "",
    val notes: String? = null
)
