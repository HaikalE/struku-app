package com.example.struku.data.ocr

import android.util.Log
import com.example.struku.domain.model.LineItem
import com.example.struku.domain.model.Receipt
import com.google.mlkit.vision.text.Text
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parser for extracting structured information from receipt OCR results
 */
@Singleton
class ReceiptParser @Inject constructor() {

    companion object {
        private const val TAG = "ReceiptParser"
        
        // Regular expressions for parsing receipt elements
        private val DATE_PATTERNS = listOf(
            Pattern.compile("\\b(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})\\b"),
            Pattern.compile("\\b(\\d{1,2}\\s+[A-Za-z]{3,}\\s+\\d{2,4})\\b"),
            Pattern.compile("\\b(\\d{1,2}\\s+[A-Za-z]{3,})\\b")
        )
        
        private val TOTAL_PATTERNS = listOf(
            Pattern.compile("(?i)total.*?(?:Rp|IDR)?\\s*(\\d+[.,]\\d{2,3})"),
            Pattern.compile("(?i)(?:total|jumlah).*?(\\d+[.,]\\d{2,3})"),
            Pattern.compile("(?i)(?:grand total|total belanja).*?(\\d+[.,]\\d{2,3})"),
            Pattern.compile("(?i)(?:jumlah bayar).*?(\\d+[.,]\\d{2,3})")
        )
        
        private val MERCHANT_PATTERNS = listOf(
            Pattern.compile("(?i)^([A-Za-z0-9\\s.]+?)\\s*(?:ltd|inc)?\\s*$", Pattern.MULTILINE),
            Pattern.compile("(?i)(?:store|toko|market):\\s*([A-Za-z0-9\\s.]+)", Pattern.MULTILINE)
        )
        
        private val ITEM_PATTERN = Pattern.compile("(?i)([A-Za-z0-9\\s.]{5,})\\s+(\\d+)\\s*[xX]\\s*(\\d+[.,]\\d{2,3})\\s*(\\d+[.,]\\d{2,3})")
    }
    
    /**
     * Extract structured receipt data from OCR Text result
     * @param text OCR result from ML Kit
     * @return Parsed Receipt object
     */
    fun parseReceipt(text: Text): Receipt {
        val rawText = text.text
        
        Log.d(TAG, "Parsing receipt text: $rawText")
        
        // Extract merchant name
        val merchantName = extractMerchantName(rawText)
        
        // Extract date
        val date = extractDate(rawText)
        
        // Extract total amount
        val total = extractTotal(rawText)
        
        // Extract line items
        val lineItems = extractLineItems(rawText)
        
        return Receipt(
            id = 0, // Will be set on DB insert
            merchantName = merchantName ?: "Unknown Merchant",
            date = date?.time ?: System.currentTimeMillis(),
            total = total ?: 0.0,
            category = "", // Will be inferred later
            notes = "",
            imagePath = null,
            items = lineItems
        )
    }
    
    /**
     * Extract merchant name from receipt text
     */
    private fun extractMerchantName(text: String): String? {
        for (pattern in MERCHANT_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return matcher.group(1)?.trim()
            }
        }
        
        // Fallback: Try to get the first non-empty line as merchant name
        val lines = text.split("\n")
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && trimmed.length > 3) {
                return trimmed
            }
        }
        
        return null
    }
    
    /**
     * Extract date from receipt text
     */
    private fun extractDate(text: String): Date? {
        // Try different date patterns
        for (pattern in DATE_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val dateStr = matcher.group(1) ?: continue
                
                // Try different date formats
                val formats = listOf(
                    "dd/MM/yyyy", "dd-MM-yyyy", "dd/MM/yy", "dd-MM-yy",
                    "dd MMM yyyy", "dd MMM"
                )
                
                for (format in formats) {
                    try {
                        val dateFormat = SimpleDateFormat(format, Locale.getDefault())
                        return dateFormat.parse(dateStr)
                    } catch (e: ParseException) {
                        // Try next format
                    }
                }
            }
        }
        
        // If no date found, return current date
        return Date()
    }
    
    /**
     * Extract total amount from receipt text
     */
    private fun extractTotal(text: String): Double? {
        for (pattern in TOTAL_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val amountStr = matcher.group(1)?.replace(",", ".")
                try {
                    return amountStr?.toDouble()
                } catch (e: NumberFormatException) {
                    // Try next pattern
                }
            }
        }
        
        return null
    }
    
    /**
     * Extract line items from receipt text
     */
    private fun extractLineItems(text: String): List<LineItem> {
        val items = mutableListOf<LineItem>()
        val matcher = ITEM_PATTERN.matcher(text)
        
        while (matcher.find()) {
            try {
                val name = matcher.group(1)?.trim() ?: continue
                val quantity = matcher.group(2)?.toDoubleOrNull() ?: 1.0
                val price = matcher.group(3)?.replace(",", ".")?.toDoubleOrNull() ?: continue
                val itemTotal = matcher.group(4)?.replace(",", ".")?.toDoubleOrNull() ?: (price * quantity)
                
                items.add(
                    LineItem(
                        id = 0, // Will be set on DB insert
                        receiptId = 0, // Will be set after receipt insert
                        name = name,
                        price = price,
                        quantity = quantity,
                        total = itemTotal
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing line item: ${e.message}")
            }
        }
        
        return items
    }
    
    /**
     * Try to infer receipt category from items
     */
    fun inferCategory(receipt: Receipt): String {
        val text = receipt.merchantName.lowercase() + " " + 
            receipt.items.joinToString(" ") { it.name.lowercase() }
        
        return when {
            containsAny(text, listOf("grocery", "market", "supermarket", "mart", "alfamart", "indomaret", "carefour")) -> "Groceries"
            containsAny(text, listOf("restaurant", "cafe", "food", "coffee", "meal", "restoran", "kopi", "makanan")) -> "Dining"
            containsAny(text, listOf("transport", "grab", "gojek", "taxi", "uber", "train", "bus", "mrt", "pesawat", "tiket")) -> "Transportation"
            containsAny(text, listOf("pharmacy", "drug", "clinic", "hospital", "doctor", "medicine", "apotek", "obat")) -> "Healthcare"
            containsAny(text, listOf("cloth", "fashion", "apparel", "shoes", "baju", "sepatu", "celana", "pakaian")) -> "Clothing"
            containsAny(text, listOf("entertainment", "movie", "cinema", "theater", "bioskop", "film")) -> "Entertainment"
            else -> "Uncategorized"
        }
    }
    
    /**
     * Check if text contains any keyword from a list
     */
    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it, ignoreCase = true) }
    }
}