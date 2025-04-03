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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of OCR Repository with optimized performance
 */
@Singleton
class OcrRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ocrEngine: MlKitOcrEngine,
    private val imagePreprocessor: AdvancedImagePreprocessor,
    private val receiptParser: ReceiptParser
) : OcrRepository {
    
    private val TAG = "OcrRepositoryImpl"
    
    // Increased timeout constants to prevent premature job cancellations
    private val OCR_TIMEOUT_MS = 45000L  // 45 seconds (increased from 30)
    private val PROCESSING_TIMEOUT_MS = 30000L  // 30 seconds (increased from 20)
    
    // Track if a processing job is running
    private val isProcessing = AtomicBoolean(false)
    
    /**
     * Parse the content of a receipt from text
     */
    override suspend fun parseReceiptText(text: String): Map<String, Any> {
        return withContext(Dispatchers.Default) {
            try {
                // Set processing flag
                isProcessing.set(true)
                
                withTimeout(PROCESSING_TIMEOUT_MS) {
                    // Periodically check if the coroutine is still active
                    ensureActive()
                    val result = receiptParser.parse(text)
                    ensureActive()
                    result
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Text parsing timeout after ${PROCESSING_TIMEOUT_MS}ms", e)
                mapOf("error" to "Proses terlalu lama. Silakan coba lagi.")
            } catch (e: CancellationException) {
                Log.e(TAG, "Text parsing was cancelled", e)
                mapOf("error" to "Proses dibatalkan.")
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing receipt text: ${e.message}", e)
                mapOf("error" to "Gagal mengurai teks struk: ${e.message}")
            } finally {
                isProcessing.set(false)
            }
        }
    }
    
    /**
     * Detect if text likely contains dollar format
     */
    private fun detectDollarFormat(text: String): Boolean {
        // Count dollar signs
        val dollarSignCount = text.count { it == '$' }
        
        // Look for price patterns with dollar signs
        val dollarPricePattern = Regex("\\$\\s*\\d+\\.\\d{2}")
        val dollarPriceMatches = dollarPricePattern.findAll(text).count()
        
        // Check if there are specific columns that might indicate a US format receipt
        val qtyPattern = Regex("(?i)\\b(qty|quantity)\\b")
        val amountPattern = Regex("(?i)\\b(amount|subtotal)\\b")
        
        return dollarSignCount > 2 || dollarPriceMatches > 2 || 
               (qtyPattern.containsMatchIn(text) && amountPattern.containsMatchIn(text))
    }
    
    /**
     * Recognize text in a receipt image
     */
    override suspend fun recognizeReceiptImage(bitmap: Bitmap): String {
        return withContext(Dispatchers.Default) {
            try {
                // Set processing flag
                isProcessing.set(true)
                
                withTimeout(OCR_TIMEOUT_MS) {
                    // Optimize by downscaling large images
                    val optimizedBitmap = if (bitmap.width > 2000 || bitmap.height > 2000) {
                        val scale = 0.5f
                        Bitmap.createScaledBitmap(
                            bitmap,
                            (bitmap.width * scale).toInt(),
                            (bitmap.height * scale).toInt(),
                            true
                        )
                    } else {
                        bitmap
                    }
                    
                    // First try with standard config
                    val standardConfig = ReceiptPreprocessingConfigFactory.createConfig()
                    ensureActive() // Check if cancelled
                    
                    val processedImage = imagePreprocessor.processReceiptImage(optimizedBitmap, standardConfig)
                    ensureActive() // Check if cancelled
                    
                    // Run OCR to get initial text
                    val initialText = ocrEngine.recognizeText(processedImage)
                    ensureActive() // Check if cancelled
                    
                    // If dollar format detected, try again with optimized config
                    if (detectDollarFormat(initialText)) {
                        Log.d(TAG, "Dollar format detected, applying specialized preprocessing")
                        val dollarConfig = ReceiptPreprocessingConfigFactory.createDollarFormatConfig()
                        val reprocessedImage = imagePreprocessor.processReceiptImage(optimizedBitmap, dollarConfig)
                        ensureActive()
                        ocrEngine.recognizeText(reprocessedImage)
                    } else {
                        initialText
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "OCR timeout after ${OCR_TIMEOUT_MS}ms", e)
                "Proses pengenalan teks terlalu lama. Silakan coba lagi."
            } catch (e: CancellationException) {
                Log.e(TAG, "OCR process was cancelled", e)
                "Proses dibatalkan."
            } catch (e: Exception) {
                Log.e(TAG, "Error recognizing text in image: ${e.message}", e)
                ""
            } finally {
                isProcessing.set(false)
                
                // Free up resources
                imagePreprocessor.clearCache()
                ocrEngine.clearCache()
            }
        }
    }
    
    /**
     * Process a receipt image with default configuration
     */
    override suspend fun processReceipt(bitmap: Bitmap): Map<String, Any> {
        return withContext(Dispatchers.Default) {
            try {
                // Set processing flag
                isProcessing.set(true)
                
                withTimeout(OCR_TIMEOUT_MS) {
                    // Optimize by downscaling large images
                    val optimizedBitmap = if (bitmap.width > 2000 || bitmap.height > 2000) {
                        val scale = 0.5f
                        Bitmap.createScaledBitmap(
                            bitmap,
                            (bitmap.width * scale).toInt(),
                            (bitmap.height * scale).toInt(),
                            true
                        )
                    } else {
                        bitmap
                    }
                    
                    // First try with standard config
                    val standardConfig = ReceiptPreprocessingConfigFactory.createConfig()
                    ensureActive() // Check if cancelled
                    
                    val processedImage = imagePreprocessor.processReceiptImage(optimizedBitmap, standardConfig)
                    ensureActive() // Check if cancelled
                    
                    // Run OCR
                    val initialText = ocrEngine.recognizeText(processedImage)
                    ensureActive() // Check if cancelled
                    
                    // Check if dollar format
                    val finalText = if (detectDollarFormat(initialText)) {
                        Log.d(TAG, "Dollar format detected during processing, applying specialized preprocessing")
                        val dollarConfig = ReceiptPreprocessingConfigFactory.createDollarFormatConfig()
                        val reprocessedImage = imagePreprocessor.processReceiptImage(optimizedBitmap, dollarConfig)
                        ensureActive()
                        ocrEngine.recognizeText(reprocessedImage)
                    } else {
                        initialText
                    }
                    
                    // Parse the results
                    receiptParser.parse(finalText)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Receipt processing timeout after ${OCR_TIMEOUT_MS}ms", e)
                mapOf("error" to "Proses terlalu lama. Silakan coba lagi.")
            } catch (e: CancellationException) {
                Log.e(TAG, "Receipt processing was cancelled", e)
                mapOf("error" to "Proses dibatalkan.")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing receipt: ${e.message}", e)
                mapOf("error" to "Gagal memproses struk: ${e.message}")
            } finally {
                isProcessing.set(false)
                
                // Free up resources
                imagePreprocessor.clearCache()
                ocrEngine.clearCache()
            }
        }
    }
    
    /**
     * Process a receipt image with custom configuration
     */
    override suspend fun processReceipt(
        bitmap: Bitmap,
        config: ReceiptPreprocessingConfig
    ): Map<String, Any> {
        return withContext(Dispatchers.Default) {
            try {
                // Set processing flag
                isProcessing.set(true)
                
                withTimeout(OCR_TIMEOUT_MS) {
                    // Optimize by downscaling large images
                    val optimizedBitmap = if (bitmap.width > 2000 || bitmap.height > 2000) {
                        val scale = 0.5f
                        Bitmap.createScaledBitmap(
                            bitmap,
                            (bitmap.width * scale).toInt(),
                            (bitmap.height * scale).toInt(),
                            true
                        )
                    } else {
                        bitmap
                    }
                    
                    // Preprocess with custom config
                    ensureActive() // Check if cancelled
                    
                    val processedImage = imagePreprocessor.processReceiptImage(optimizedBitmap, config)
                    ensureActive() // Check if cancelled
                    
                    // Run OCR
                    val text = ocrEngine.recognizeText(processedImage)
                    ensureActive() // Check if cancelled
                    
                    // Parse the results
                    receiptParser.parse(text)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Receipt processing timeout after ${OCR_TIMEOUT_MS}ms", e)
                mapOf("error" to "Proses terlalu lama. Silakan coba lagi.")
            } catch (e: CancellationException) {
                Log.e(TAG, "Receipt processing with custom config was cancelled", e)
                mapOf("error" to "Proses dibatalkan.")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing receipt with custom config: ${e.message}", e)
                mapOf("error" to "Gagal memproses struk: ${e.message}")
            } finally {
                isProcessing.set(false)
                
                // Free up resources
                imagePreprocessor.clearCache()
                ocrEngine.clearCache()
            }
        }
    }
    
    /**
     * Scan a receipt image into a Receipt object
     */
    override suspend fun scanReceipt(bitmap: Bitmap): Receipt? {
        return withContext(Dispatchers.Default) {
            try {
                // Set processing flag
                isProcessing.set(true)
                
                withTimeout(OCR_TIMEOUT_MS) {
                    // Process the receipt
                    val extractedData = processReceipt(bitmap)
                    ensureActive() // Check if cancelled
                    
                    // Check for errors
                    if (extractedData.containsKey("error")) {
                        return@withTimeout null
                    }
                    
                    // Convert the extracted data to a Receipt object
                    val merchantName = extractedData["merchantName"] as? String ?: "Unknown Merchant"
                    val total = extractedData["total"] as? Double ?: 0.0
                    val date = extractedData["date"] as? Date ?: Date()
                    
                    // Get items if available
                    @Suppress("UNCHECKED_CAST")
                    val itemsList = extractedData["items"] as? List<Map<String, Any>> ?: emptyList()
                    
                    // Convert generic item maps to LineItem objects
                    val lineItems = itemsList.map { itemMap ->
                        com.example.struku.domain.model.LineItem(
                            name = itemMap["name"] as String,
                            price = itemMap["price"] as Double,
                            quantity = itemMap["quantity"] as Double,
                            unitPrice = itemMap["price"] as Double
                        )
                    }
                    
                    // Create and return a Receipt object
                    Receipt(
                        merchantName = merchantName,
                        total = total,
                        date = date,
                        imageUri = null,
                        items = lineItems,
                        category = extractedData["category"] as? String ?: ""
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Receipt scanning timeout after ${OCR_TIMEOUT_MS}ms", e)
                null
            } catch (e: CancellationException) {
                Log.e(TAG, "Receipt scanning was cancelled", e)
                null
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning receipt: ${e.message}", e)
                null
            } finally {
                isProcessing.set(false)
                
                // Free up resources
                imagePreprocessor.clearCache()
                ocrEngine.clearCache()
            }
        }
    }
    
    /**
     * Save a receipt image to storage
     */
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
                
                // Compress the bitmap to reduce storage size
                FileOutputStream(imageFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos) // Reduced quality to save space
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
    
    /**
     * Check if processing is currently running
     */
    fun isProcessingActive(): Boolean {
        return isProcessing.get()
    }
}