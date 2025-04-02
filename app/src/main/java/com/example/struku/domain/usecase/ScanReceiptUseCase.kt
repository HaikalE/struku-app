package com.example.struku.domain.usecase

import android.graphics.Bitmap
import com.example.struku.domain.model.Receipt
import com.example.struku.domain.repository.OcrRepository
import com.example.struku.domain.repository.ReceiptRepository
import javax.inject.Inject

/**
 * Use case untuk memindai struk dari gambar
 */
class ScanReceiptUseCase @Inject constructor(
    private val ocrRepository: OcrRepository,
    private val receiptRepository: ReceiptRepository
) {
    /**
     * Memindai struk dari gambar dan mengembalikan objek Receipt
     * @param image Gambar struk yang akan dipindai
     * @return Receipt hasil pemindaian atau null jika gagal
     */
    suspend operator fun invoke(image: Bitmap): Receipt? {
        return ocrRepository.scanReceipt(image)
    }
    
    /**
     * Memindai dan langsung menyimpan struk ke database
     * @param image Gambar struk
     * @return ID dari struk yang disimpan atau null jika gagal
     */
    suspend fun scanAndSave(image: Bitmap): Long? {
        val receipt = ocrRepository.scanReceipt(image) ?: return null
        val receiptId = receiptRepository.saveReceipt(receipt)
        
        // Simpan gambar jika berhasil menyimpan receipt
        if (receiptId > 0) {
            val imageUri = ocrRepository.saveReceiptImage(image, receiptId)
            val updatedReceipt = receipt.copy(id = receiptId.toInt(), imageUri = imageUri)
            receiptRepository.updateReceipt(updatedReceipt)
        }
        
        return receiptId
    }
}