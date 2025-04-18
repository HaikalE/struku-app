package com.example.struku.domain.repository

import android.graphics.Bitmap
import com.example.struku.data.ocr.ReceiptPreprocessingConfig
import com.example.struku.domain.model.Receipt

/**
 * Repository interface for OCR operations
 */
interface OcrRepository {
    
    /**
     * Parse receipt text to extract structured data
     * 
     * @param text OCR recognized text
     * @return Map of extracted data fields
     */
    suspend fun parseReceiptText(text: String): Map<String, Any>
    
    /**
     * Recognize text from a receipt image
     * 
     * @param bitmap Image to process
     * @return Recognized text
     */
    suspend fun recognizeReceiptImage(bitmap: Bitmap): String
    
    /**
     * Process a receipt image to extract structured data
     * Uses default preprocessing configuration
     * 
     * @param bitmap Image to process
     * @return Map of extracted data fields
     */
    suspend fun processReceipt(bitmap: Bitmap): Map<String, Any>
    
    /**
     * Process a receipt image with custom preprocessing config
     * 
     * @param bitmap Image to process
     * @param config Custom preprocessing configuration
     * @return Map of extracted data fields
     */
    suspend fun processReceipt(
        bitmap: Bitmap,
        config: ReceiptPreprocessingConfig
    ): Map<String, Any>
    
    /**
     * Scan a receipt image and convert to Receipt object
     * 
     * @param bitmap Image to scan
     * @return Receipt object or null if scan failed
     */
    suspend fun scanReceipt(bitmap: Bitmap): Receipt?
    
    /**
     * Save a receipt image to storage
     * 
     * @param bitmap Image to save
     * @param receiptId ID of the receipt
     * @return URI of the saved image as a string
     */
    suspend fun saveReceiptImage(bitmap: Bitmap, receiptId: Long): String?
}