package com.example.struku.di

import com.example.struku.data.ocr.MlKitOcrEngine
import com.example.struku.data.ocr.ReceiptParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OcrModule {

    @Provides
    @Singleton
    fun provideMlKitOcrEngine(): MlKitOcrEngine {
        return MlKitOcrEngine()
    }

    @Provides
    @Singleton
    fun provideReceiptParser(): ReceiptParser {
        return ReceiptParser()
    }
}
