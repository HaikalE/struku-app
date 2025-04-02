package com.example.struku.di

import android.content.Context
import com.example.struku.data.local.dao.BudgetDao
import com.example.struku.data.local.dao.CategoryDao
import com.example.struku.data.local.dao.LineItemDao
import com.example.struku.data.local.dao.ReceiptDao
import com.example.struku.data.ocr.MlKitOcrEngine
import com.example.struku.data.ocr.ReceiptParser
import com.example.struku.data.repository.BudgetRepositoryImpl
import com.example.struku.data.repository.CategoryRepositoryImpl
import com.example.struku.data.repository.OcrRepositoryImpl
import com.example.struku.data.repository.ReceiptRepositoryImpl
import com.example.struku.domain.repository.BudgetRepository
import com.example.struku.domain.repository.CategoryRepository
import com.example.struku.domain.repository.OcrRepository
import com.example.struku.domain.repository.ReceiptRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideReceiptRepository(
        receiptDao: ReceiptDao,
        lineItemDao: LineItemDao
    ): ReceiptRepository {
        return ReceiptRepositoryImpl(receiptDao, lineItemDao)
    }

    @Provides
    @Singleton
    fun provideCategoryRepository(
        categoryDao: CategoryDao
    ): CategoryRepository {
        return CategoryRepositoryImpl(categoryDao)
    }

    @Provides
    @Singleton
    fun provideBudgetRepository(
        budgetDao: BudgetDao
    ): BudgetRepository {
        return BudgetRepositoryImpl(budgetDao)
    }

    @Provides
    @Singleton
    fun provideOcrRepository(
        mlKitOcrEngine: MlKitOcrEngine,
        receiptParser: ReceiptParser,
        @ApplicationContext context: Context
    ): OcrRepository {
        return OcrRepositoryImpl(mlKitOcrEngine, receiptParser, context)
    }
}
