package com.example.struku.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.struku.domain.model.Receipt
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Utility class for exporting data to CSV and PDF
 */
class DataExporter(private val context: Context) {

    /**
     * Export receipts to CSV file
     * @return Uri to the exported file, or null if export failed
     */
    fun exportReceiptsToCsv(receipts: List<Receipt>): Uri? {
        if (receipts.isEmpty()) return null
        
        try {
            // Create export directory if it doesn't exist
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            // Create CSV file
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val timestamp = dateFormat.format(Date())
            val outputFile = File(exportDir, "struku_export_${timestamp}.csv")
            
            FileWriter(outputFile).use { writer ->
                // Write header
                writer.append("Merchant,Date,Category,Total,Currency,Items\n")
                
                // Write receipts
                val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                receipts.forEach { receipt ->
                    val date = dateFormatter.format(receipt.date)
                    val items = receipt.items.joinToString("|")
                        { "${it.description} (${it.quantity} x ${it.price})" }
                        .replace(",", ";")
                    
                    writer.append(
                        "${receipt.merchantName.replace(",", ";")}," +
                        "${date}," +
                        "${receipt.category}," +
                        "${receipt.total}," +
                        "${receipt.currency}," +
                        "\"${items}\"\n"
                    )
                }
            }
            
            // Return content URI through FileProvider
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Share exported data using Android share sheet
     */
    fun shareExportedFile(uri: Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val shareIntent = Intent.createChooser(intent, "Bagikan data ekspor")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
}