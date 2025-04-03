package com.example.struku.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.struku.data.local.converter.DateConverter
import com.example.struku.data.local.dao.BudgetDao
import com.example.struku.data.local.dao.CategoryDao
import com.example.struku.data.local.dao.ReceiptDao
import com.example.struku.data.local.entity.BudgetEntity
import com.example.struku.data.local.entity.CategoryEntity
import com.example.struku.data.local.entity.LineItemEntity
import com.example.struku.data.local.entity.ReceiptEntity

/**
 * Main database for the StruKu application
 */
@Database(
    entities = [
        ReceiptEntity::class,
        LineItemEntity::class,
        CategoryEntity::class,
        BudgetEntity::class
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

    /**
     * Get the DAO for category operations
     */
    abstract fun categoryDao(): CategoryDao
    
    /**
     * Get the DAO for budget operations
     */
    abstract fun budgetDao(): BudgetDao
}
