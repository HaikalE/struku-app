package com.example.struku.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    indices = [Index("receiptId")]
)
data class LineItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val receiptId: Int,
    val description: String,
    val quantity: Int,
    val unitPrice: Double?,
    val price: Double,
    val category: String?
)
