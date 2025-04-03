package com.example.struku.di

import android.content.Context
import com.example.struku.data.local.dao.BudgetDao
import com.example.struku.data.local.dao.CategoryDao
import com.example.struku.data.local.dao.ReceiptDao
import com.example.struku.data.mapper.ReceiptMapper
import com.example.struku.data.ocr.AdvancedImagePreprocessor
import com.example.struku.data.ocr.MlKitOcrEngine
import com.example.struku.data.ocr.PreprocessingVisualizer
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

/**
 * Dependency injection module for repository-related components
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    /**
     * Provide receipt repository implementation
     */
    @Provides
    @Singleton
    fun provideReceiptRepository(
        receiptDao: ReceiptDao,
        receiptMapper: ReceiptMapper
    ): ReceiptRepository {
        return ReceiptRepositoryImpl(receiptDao, receiptMapper)
    }
    
    /**
     * Provide OCR repository implementation
     */
    @Provides
    @Singleton
    fun provideOcrRepository(
        @ApplicationContext context: Context,
        ocrEngine: MlKitOcrEngine,
        imagePreprocessor: AdvancedImagePreprocessor,
        receiptParser: ReceiptParser
    ): OcrRepository {
        return OcrRepositoryImpl(context, ocrEngine, imagePreprocessor, receiptParser)
    }
    
    /**
     * Provide receipt mapper
     */
    @Provides
    @Singleton
    fun provideReceiptMapper(): ReceiptMapper {
        return ReceiptMapper()
    }
    
    /**
     * Provide budget repository implementation
     */
    @Provides
    @Singleton
    fun provideBudgetRepository(
        budgetDao: BudgetDao
    ): BudgetRepository {
        return BudgetRepositoryImpl(budgetDao)
    }
    
    /**
     * Provide category repository implementation
     */
    @Provides
    @Singleton
    fun provideCategoryRepository(
        categoryDao: CategoryDao
    ): CategoryRepository {
        return CategoryRepositoryImpl(categoryDao)
    }
    
    /* Removed duplicate provider for ReceiptParser - now only in OcrModule */
}
