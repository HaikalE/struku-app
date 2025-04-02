package com.example.struku.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.struku.data.ocr.MlKitOcrEngine
import com.example.struku.data.ocr.ReceiptParser
import com.example.struku.domain.model.Receipt
import com.example.struku.domain.repository.OcrRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

class OcrRepositoryImpl @Inject constructor(
    private val mlKitOcrEngine: MlKitOcrEngine,
    private val receiptParser: ReceiptParser,
    @ApplicationContext private val context: Context
) : OcrRepository {

    override suspend fun recognizeText(image: Bitmap): String {
        return mlKitOcrEngine.recognizeTextAsString(image)
    }

    override suspend fun parseReceiptText(text: String): Receipt? {
        return receiptParser.parseReceiptText(text)
    }

    override suspend fun scanReceipt(image: Bitmap): Receipt? {
        val text = recognizeText(image)
        return parseReceiptText(text)
    }

    override suspend fun saveReceiptImage(image: Bitmap, receiptId: Long): String {
        val dir = File(context.filesDir, "receipts")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        
        // Create a file with unique name based on receipt ID
        val file = File(dir, "receipt_${receiptId}_${UUID.randomUUID()}.jpg")
        
        try {
            FileOutputStream(file).use { out ->
                // Compress and save the image
                image.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
            }
            
            // Return the URI as string
            return file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }
}
