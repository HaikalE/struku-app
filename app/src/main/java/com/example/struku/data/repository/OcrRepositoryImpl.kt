package com.example.struku.data.repository

import android.graphics.Bitmap
import android.util.Log
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
    
    private val TAG = "OcrRepositoryImpl"
    
    override suspend fun parseReceiptText(text: String): Map<String, Any> {
        return withContext(Dispatchers.Default) {
            receiptParser.parse(text)
        }
    }
    
    override suspend fun recognizeReceiptImage(bitmap: Bitmap): String {
        return withContext(Dispatchers.Default) {
            try {
                // First preprocess the image
                val config = ReceiptPreprocessingConfigFactory.createConfig()
                val processedImage = imagePreprocessor.processReceiptImage(bitmap, config)
                
                // Then run OCR on it
                ocrEngine.recognizeText(processedImage)
            } catch (e: Exception) {
                Log.e(TAG, "Error recognizing text in image: ${e.message}", e)
                ""
            }
        }
    }
    
    override suspend fun processReceipt(bitmap: Bitmap): Map<String, Any> {
        return withContext(Dispatchers.Default) {
            try {
                // Preprocess the image
                val config = ReceiptPreprocessingConfigFactory.createConfig()
                val processedImage = imagePreprocessor.processReceiptImage(bitmap, config)
                
                // Run OCR
                val text = ocrEngine.recognizeText(processedImage)
                
                // Parse the results
                receiptParser.parse(text)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing receipt: ${e.message}", e)
                mapOf("error" to "Failed to process receipt: ${e.message}")
            }
        }
    }
    
    override suspend fun processReceipt(
        bitmap: Bitmap,
        config: ReceiptPreprocessingConfig
    ): Map<String, Any> {
        return withContext(Dispatchers.Default) {
            try {
                // Preprocess with custom config
                val processedImage = imagePreprocessor.processReceiptImage(bitmap, config)
                
                // Run OCR
                val text = ocrEngine.recognizeText(processedImage)
                
                // Parse the results
                receiptParser.parse(text)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing receipt with custom config: ${e.message}", e)
                mapOf("error" to "Failed to process receipt: ${e.message}")
            }
        }
    }
}
