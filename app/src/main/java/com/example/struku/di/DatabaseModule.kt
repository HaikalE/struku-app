package com.example.struku.di

import android.content.Context
import android.util.Log
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

    private const val TAG = "DatabaseModule"

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        // Create encryption key for SQLCipher
        // In a real app, this should be securely stored/generated
        val passphrase = SQLiteDatabase.getBytes("struku-password".toCharArray())
        val factory = SupportFactory(passphrase)
        
        Log.d(TAG, "Creating AppDatabase instance")
        
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
    @Singleton
    fun provideReceiptDao(database: AppDatabase): ReceiptDao {
        Log.d(TAG, "Providing ReceiptDao")
        return database.receiptDao()
    }

    @Provides
    @Singleton
    fun provideLineItemDao(database: AppDatabase): LineItemDao {
        Log.d(TAG, "Providing LineItemDao")
        return database.lineItemDao()
    }

    @Provides
    @Singleton
    fun provideCategoryDao(database: AppDatabase): CategoryDao {
        Log.d(TAG, "Providing CategoryDao")
        return database.categoryDao()
    }

    @Provides
    @Singleton
    fun provideBudgetDao(database: AppDatabase): BudgetDao {
        Log.d(TAG, "Providing BudgetDao")
        return database.budgetDao()
    }
}
