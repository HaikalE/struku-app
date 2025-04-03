package com.example.struku.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.struku.data.ocr.AdvancedImagePreprocessor
import com.example.struku.data.ocr.MlKitOcrEngine
import com.example.struku.data.ocr.ReceiptParser
import com.example.struku.data.ocr.ReceiptPreprocessingConfig
import com.example.struku.data.ocr.ReceiptPreprocessingConfigFactory
import com.example.struku.domain.model.Receipt
import com.example.struku.domain.repository.OcrRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of OCR Repository
 */
@Singleton
class OcrRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
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
    
    override suspend fun scanReceipt(bitmap: Bitmap): Receipt? {
        return withContext(Dispatchers.Default) {
            try {
                // Process the receipt
                val extractedData = processReceipt(bitmap)
                
                // Convert the extracted data to a Receipt object
                val merchantName = extractedData["merchantName"] as? String ?: "Unknown Merchant"
                val total = extractedData["total"] as? Double ?: 0.0
                val date = extractedData["date"] as? Date ?: Date()
                
                // Create and return a Receipt object
                Receipt(
                    merchantName = merchantName,
                    total = total,
                    date = date,
                    imageUri = null,
                    items = emptyList(),
                    category = extractedData["category"] as? String ?: ""
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning receipt: ${e.message}", e)
                null
            }
        }
    }
    
    override suspend fun saveReceiptImage(bitmap: Bitmap, receiptId: Long): String? {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val filename = "Receipt_${receiptId}_${timestamp}.jpg"
                
                // Get the directory for saving the image
                val imagesDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "receipts")
                if (!imagesDir.exists()) {
                    imagesDir.mkdirs()
                }
                
                // Create file
                val imageFile = File(imagesDir, filename)
                
                // Save the bitmap to the file
                FileOutputStream(imageFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                    fos.flush()
                }
                
                // Return the file path as a string
                imageFile.absolutePath
                
            } catch (e: Exception) {
                Log.e(TAG, "Error saving receipt image: ${e.message}", e)
                null
            }
        }
    }
}