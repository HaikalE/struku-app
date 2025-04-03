package com.example.struku.data.repository

import android.graphics.Bitmap
import com.example.struku.data.ocr.AdvancedImagePreprocessor
import com.example.struku.data.ocr.MlKitOcrEngine
import com.example.struku.data.ocr.ReceiptParser
import com.example.struku.data.ocr.ReceiptPreprocessingConfig
import com.example.struku.data.ocr.ReceiptPreprocessingConfigFactory
import com.example.struku.domain.repository.OcrRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of OCR Repository
 */
@Singleton
class OcrRepositoryImpl @Inject constructor(
    private val ocrEngine: MlKitOcrEngine,
    private val imagePreprocessor: AdvancedImagePreprocessor,
    private val receiptParser: ReceiptParser
) : OcrRepository {
    
    override suspend fun parseReceiptText(text: String): Map<String, Any> {
        return withContext(Dispatchers.Default) {
            receiptParser.parse(text)
        }
    }
    
    override suspend fun recognizeReceiptImage(bitmap: Bitmap): String {
        return withContext(Dispatchers.Default) {
            // First preprocess the image
            val config = ReceiptPreprocessingConfigFactory.createBalancedConfig()
            val processedImage = imagePreprocessor.processReceiptImage(bitmap, config)
            
            // Then run OCR on it
            ocrEngine.recognizeText(processedImage)
        }
    }
    
    override suspend fun processReceipt(bitmap: Bitmap): Map<String, Any> {
        return withContext(Dispatchers.Default) {
            // Preprocess the image
            val config = ReceiptPreprocessingConfigFactory.createBalancedConfig()
            val processedImage = imagePreprocessor.processReceiptImage(bitmap, config)
            
            // Run OCR
            val text = ocrEngine.recognizeText(processedImage)
            
            // Parse the results
            receiptParser.parse(text)
        }
    }
    
    override suspend fun processReceipt(
        bitmap: Bitmap,
        config: ReceiptPreprocessingConfig
    ): Map<String, Any> {
        return withContext(Dispatchers.Default) {
            // Preprocess with custom config
            val processedImage = imagePreprocessor.processReceiptImage(bitmap, config)
            
            // Run OCR
            val text = ocrEngine.recognizeText(processedImage)
            
            // Parse the results
            receiptParser.parse(text)
        }
    }
}
