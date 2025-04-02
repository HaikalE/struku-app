package com.example.struku.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.struku.data.local.dao.BudgetDao
import com.example.struku.data.local.dao.CategoryDao
import com.example.struku.data.local.dao.LineItemDao
import com.example.struku.data.local.dao.ReceiptDao
import com.example.struku.data.local.entity.BudgetEntity
import com.example.struku.data.local.entity.CategoryEntity
import com.example.struku.data.local.entity.LineItemEntity
import com.example.struku.data.local.entity.ReceiptEntity

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
@TypeConverters(Converters::class)
abbstract class AppDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao
    abstract fun lineItemDao(): LineItemDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
}
