package com.example.struku.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.struku.data.local.converter.DateConverter
import com.example.struku.data.local.dao.ReceiptDao
import com.example.struku.data.local.entity.LineItemEntity
import com.example.struku.data.local.entity.ReceiptEntity

/**
 * Main database for the StruKu application
 */
@Database(
    entities = [
        ReceiptEntity::class,
        LineItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class StrukuDatabase : RoomDatabase() {
    /**
     * Get the DAO for receipt operations
     */
    abstract fun receiptDao(): ReceiptDao
}
