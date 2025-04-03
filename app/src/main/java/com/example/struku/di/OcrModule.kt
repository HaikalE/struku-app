package com.example.struku.di

import android.content.Context
import com.example.struku.data.ocr.AdvancedImagePreprocessor
import com.example.struku.data.ocr.MlKitOcrEngine
import com.example.struku.data.ocr.PreprocessingVisualizer
import com.example.struku.data.ocr.ReceiptParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Module for providing OCR related dependencies
 * Updated with optimized components
 */
@Module
@InstallIn(SingletonComponent::class)
object OcrModule {

    @Provides
    @Singleton
    fun providePreprocessingVisualizer(@ApplicationContext context: Context): PreprocessingVisualizer {
        val visualizer = PreprocessingVisualizer(context)
        // Initialize with optimized settings
        visualizer.setDebugMode(false) // Disable debug mode by default
        visualizer.setSamplingRate(3) // Only capture every 3rd step to reduce overhead
        return visualizer
    }

    @Provides
    @Singleton
    fun provideAdvancedImagePreprocessor(
        @ApplicationContext context: Context,
        visualizer: PreprocessingVisualizer
    ): AdvancedImagePreprocessor {
        return AdvancedImagePreprocessor(context, visualizer)
    }

    @Provides
    @Singleton
    fun provideMlKitOcrEngine(
        @ApplicationContext context: Context
    ): MlKitOcrEngine {
        return MlKitOcrEngine(context)
    }

    @Provides
    @Singleton
    fun provideReceiptParser(): ReceiptParser {
        return ReceiptParser()
    }
}
