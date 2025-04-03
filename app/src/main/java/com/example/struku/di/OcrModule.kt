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

@Module
@InstallIn(SingletonComponent::class)
object OcrModule {

    @Provides
    @Singleton
    fun providePreprocessingVisualizer(): PreprocessingVisualizer {
        return PreprocessingVisualizer()
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
    fun provideMlKitOcrEngine(imagePreprocessor: AdvancedImagePreprocessor): MlKitOcrEngine {
        return MlKitOcrEngine(imagePreprocessor)
    }

    @Provides
    @Singleton
    fun provideReceiptParser(): ReceiptParser {
        return ReceiptParser()
    }
}