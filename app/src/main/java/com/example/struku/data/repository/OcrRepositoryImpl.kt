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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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
    
    // Konstanta timeout untuk mencegah pemrosesan yang terlalu lama
    private val OCR_TIMEOUT_MS = 15000L // 15 detik
    private val PROCESSING_TIMEOUT_MS = 10000L // 10 detik
    
    override suspend fun parseReceiptText(text: String): Map<String, Any> {
        return withContext(Dispatchers.Default) {
            try {
                withTimeout(PROCESSING_TIMEOUT_MS) {
                    receiptParser.parse(text)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Text parsing timeout after ${PROCESSING_TIMEOUT_MS}ms", e)
                mapOf("error" to "Proses terlalu lama. Silakan coba lagi.")
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing receipt text: ${e.message}", e)
                mapOf("error" to "Gagal mengurai teks struk: ${e.message}")
            }
        }
    }
    
    override suspend fun recognizeReceiptImage(bitmap: Bitmap): String {
        return withContext(Dispatchers.Default) {
            try {
                withTimeout(OCR_TIMEOUT_MS) {
                    // First preprocess the image
                    val config = ReceiptPreprocessingConfigFactory.createConfig()
                    val processedImage = imagePreprocessor.processReceiptImage(bitmap, config)
                    
                    // Then run OCR on it
                    ocrEngine.recognizeText(processedImage)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "OCR timeout after ${OCR_TIMEOUT_MS}ms", e)
                "Proses pengenalan teks terlalu lama. Silakan coba lagi."
            } catch (e: Exception) {
                Log.e(TAG, "Error recognizing text in image: ${e.message}", e)
                ""
            }
        }
    }
    
    override suspend fun processReceipt(bitmap: Bitmap): Map<String, Any> {
        return withContext(Dispatchers.Default) {
            try {
                withTimeout(OCR_TIMEOUT_MS) {
                    // Preprocess the image
                    val config = ReceiptPreprocessingConfigFactory.createConfig()
                    val processedImage = imagePreprocessor.processReceiptImage(bitmap, config)
                    
                    // Run OCR
                    val text = ocrEngine.recognizeText(processedImage)
                    
                    // Parse the results
                    receiptParser.parse(text)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Receipt processing timeout after ${OCR_TIMEOUT_MS}ms", e)
                mapOf("error" to "Proses terlalu lama. Silakan coba lagi.")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing receipt: ${e.message}", e)
                mapOf("error" to "Gagal memproses struk: ${e.message}")
            }
        }
    }
    
    override suspend fun processReceipt(
        bitmap: Bitmap,
        config: ReceiptPreprocessingConfig
    ): Map<String, Any> {
        return withContext(Dispatchers.Default) {
            try {
                withTimeout(OCR_TIMEOUT_MS) {
                    // Preprocess with custom config
                    val processedImage = imagePreprocessor.processReceiptImage(bitmap, config)
                    
                    // Run OCR
                    val text = ocrEngine.recognizeText(processedImage)
                    
                    // Parse the results
                    receiptParser.parse(text)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Receipt processing timeout after ${OCR_TIMEOUT_MS}ms", e)
                mapOf("error" to "Proses terlalu lama. Silakan coba lagi.")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing receipt with custom config: ${e.message}", e)
                mapOf("error" to "Gagal memproses struk: ${e.message}")
            }
        }
    }
    
    override suspend fun scanReceipt(bitmap: Bitmap): Receipt? {
        return withContext(Dispatchers.Default) {
            try {
                withTimeout(OCR_TIMEOUT_MS) {
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
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Receipt scanning timeout after ${OCR_TIMEOUT_MS}ms", e)
                null
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
