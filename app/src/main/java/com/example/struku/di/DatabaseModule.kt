package com.example.struku.di

import android.content.Context
import androidx.room.Room
import com.example.struku.data.local.AppDatabase
import com.example.struku.data.local.dao.BudgetDao
import com.example.struku.data.local.dao.CategoryDao
import com.example.struku.data.local.dao.LineItemDao
import com.example.struku.data.local.dao.ReceiptDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        // Create encryption key for SQLCipher
        // In a real app, this should be securely stored/generated
        val passphrase = SQLiteDatabase.getBytes("struku-password".toCharArray())
        val factory = SupportFactory(passphrase)
        
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "struku-db"
        )
        .openHelperFactory(factory)  // Apply encryption
        .fallbackToDestructiveMigration()  // For development only
        .build()
    }

    @Provides
    fun provideReceiptDao(database: AppDatabase): ReceiptDao = database.receiptDao()

    @Provides
    fun provideLineItemDao(database: AppDatabase): LineItemDao = database.lineItemDao()

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideBudgetDao(database: AppDatabase): BudgetDao = database.budgetDao()
}
