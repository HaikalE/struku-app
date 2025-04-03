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
    private val OCR_TIMEOUT_MS = 30000L  // 30 seconds (increased from 15)
    private val PROCESSING_TIMEOUT_MS = 20000L  // 20 seconds (increased from 10)
    
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
                    
                    // First preprocess the image
                    val config = ReceiptPreprocessingConfigFactory.createConfig()
                    ensureActive() // Check if cancelled
                    
                    val processedImage = imagePreprocessor.processReceiptImage(optimizedBitmap, config)
                    ensureActive() // Check if cancelled
                    
                    // Then run OCR on it
                    ocrEngine.recognizeText(processedImage)
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
                    
                    // Preprocess the image
                    val config = ReceiptPreprocessingConfigFactory.createConfig()
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
                    
                    // Create and return a Receipt object
                    Receipt(
                        merchantName = merchantName,
                        total = total,
                        date = date,
                        imageUri = null,
                        items = emptyList(),
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
