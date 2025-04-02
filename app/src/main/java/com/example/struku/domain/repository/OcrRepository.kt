package com.example.struku.domain.repository

import android.graphics.Bitmap
import com.example.struku.domain.model.Receipt

/**
 * Repository interface untuk operasi OCR
 */
interface OcrRepository {
    /**
     * Menjalankan OCR pada gambar dan mengekstrak teks
     */
    suspend fun recognizeText(image: Bitmap): String
    
    /**
     * Mengurai teks OCR menjadi objek struk
     */
    suspend fun parseReceiptText(text: String): Receipt?
    
    /**
     * Menjalankan OCR dan parsing dalam satu langkah
     */
    suspend fun scanReceipt(image: Bitmap): Receipt?
    
    /**
     * Menyimpan gambar struk ke penyimpanan lokal
     * @return URI dari gambar yang disimpan
     */
    suspend fun saveReceiptImage(image: Bitmap, receiptId: Long): String
}