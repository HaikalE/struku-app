package com.example.struku.di

import android.content.Context
import androidx.room.Room
import com.example.struku.data.local.StrukuDatabase
import com.example.struku.data.local.dao.ReceiptDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for database-related components
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * Provide Room database instance
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StrukuDatabase {
        return Room.databaseBuilder(
            context,
            StrukuDatabase::class.java,
            "struku_database"
        ).build()
    }
    
    /**
     * Provide receipt DAO
     */
    @Provides
    @Singleton
    fun provideReceiptDao(database: StrukuDatabase): ReceiptDao {
        return database.receiptDao()
    }
}
