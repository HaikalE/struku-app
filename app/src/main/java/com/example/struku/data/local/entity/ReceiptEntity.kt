package com.example.struku.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.struku.data.local.converter.DateConverter
import java.util.Date

/**
 * Database entity for receipts
 */
@Entity(tableName = "receipts")
@TypeConverters(DateConverter::class)
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val merchantName: String,
    val date: Date,
    val total: Double,
    val currency: String,
    val category: String,
    val imageUri: String? = null,
    val notes: String? = null,
    val createdAt: Date,
    val updatedAt: Date
)
