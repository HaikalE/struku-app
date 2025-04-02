package com.example.struku.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val merchantName: String,
    val date: Date,
    val total: Double,
    val currency: String,
    val category: String,
    val imageUri: String?,
    val notes: String?,
    val createdAt: Date,
    val updatedAt: Date
)
