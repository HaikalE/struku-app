package com.example.struku.di

import android.content.Context
import com.example.struku.data.local.dao.ReceiptDao
import com.example.struku.data.mapper.ReceiptMapper
import com.example.struku.data.ocr.AdvancedImagePreprocessor
import com.example.struku.data.ocr.MlKitOcrEngine
import com.example.struku.data.ocr.PreprocessingVisualizer
import com.example.struku.data.ocr.ReceiptParser
import com.example.struku.data.repository.OcrRepositoryImpl
import com.example.struku.data.repository.ReceiptRepositoryImpl
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
        ocrEngine: MlKitOcrEngine,
        imagePreprocessor: AdvancedImagePreprocessor,
        receiptParser: ReceiptParser
    ): OcrRepository {
        return OcrRepositoryImpl(ocrEngine, imagePreprocessor, receiptParser)
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
     * Provide ML Kit OCR engine
     */
    @Provides
    @Singleton
    fun provideMlKitOcrEngine(@ApplicationContext context: Context): MlKitOcrEngine {
        return MlKitOcrEngine(context)
    }
    
    /**
     * Provide preprocessing visualizer
     */
    @Provides
    @Singleton
    fun providePreprocessingVisualizer(@ApplicationContext context: Context): PreprocessingVisualizer {
        return PreprocessingVisualizer(context)
    }
    
    /**
     * Provide advanced image preprocessor
     */
    @Provides
    @Singleton
    fun provideAdvancedImagePreprocessor(
        @ApplicationContext context: Context,
        visualizer: PreprocessingVisualizer
    ): AdvancedImagePreprocessor {
        return AdvancedImagePreprocessor(context, visualizer)
    }
    
    /**
     * Provide receipt parser
     */
    @Provides
    @Singleton
    fun provideReceiptParser(): ReceiptParser {
        return ReceiptParser()
    }
}