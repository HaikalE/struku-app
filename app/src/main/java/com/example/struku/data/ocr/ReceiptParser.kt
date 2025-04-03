package com.example.struku.data.ocr

import android.util.Log
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parser for OCR text extracted from receipts
 */
@Singleton
class ReceiptParser @Inject constructor() {
    private val TAG = "ReceiptParser"
    
    /**
     * Parse OCR text into structured data
     * 
     * @param text Raw OCR text
     * @return Map of extracted data
     */
    fun parse(text: String): Map<String, Any> {
        Log.d(TAG, "Parsing receipt text: ${text.take(100)}...")
        
        return try {
            // Extract key information
            val merchantName = extractMerchantName(text)
            val date = extractDate(text)
            val total = extractTotal(text)
            val items = extractLineItems(text)
            val currency = extractCurrency(text)
            
            // Build result map
            val result = mutableMapOf<String, Any>()
            result["merchantName"] = merchantName
            result["date"] = date
            result["total"] = total
            result["items"] = items
            result["currency"] = currency
            
            // Additional data
            val notes = extractNotes(text)
            if (notes.isNotEmpty()) {
                result["notes"] = notes
            }
            
            val category = categorizeReceipt(merchantName, text)
            if (category.isNotEmpty()) {
                result["category"] = category
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing receipt text: ${e.message}", e)
            mapOf(
                "error" to "Failed to parse receipt",
                "errorMessage" to (e.message ?: "Unknown error")
            )
        }
    }
    
    /**
     * Extract merchant name from receipt text
     */
    private fun extractMerchantName(text: String): String {
        // Start with first few lines, merchant name is usually at the top
        val lines = text.split("\n").take(5)
        
        // Try to find the most prominent line
        val candidateLines = lines.filter { 
            it.length > 3 && 
            !it.contains("RECEIPT") && 
            !it.contains("INVOICE") && 
            !it.matches(Regex(".*(\\d{2}/\\d{2}/\\d{2,4}).*")) 
        }
        
        // Return the first good candidate or a default value
        return candidateLines.firstOrNull()?.trim() ?: "Unknown Merchant"
    }
    
    /**
     * Extract date from receipt text
     */
    private fun extractDate(text: String): Date {
        // Look for common date formats
        val datePatterns = listOf(
            // DD/MM/YYYY
            Pattern.compile("(\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4})"),
            // YYYY/MM/DD
            Pattern.compile("(\\d{4}[/.-]\\d{1,2}[/.-]\\d{1,2})"),
            // Text representations
            Pattern.compile("(\\d{1,2}\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{2,4})", Pattern.CASE_INSENSITIVE)
        )
        
        // Try each pattern
        for (pattern in datePatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val dateStr = matcher.group(1)
                
                // Try different date formats
                val dateFormats = listOf(
                    "dd/MM/yyyy", "d/M/yyyy", "yyyy/MM/dd",
                    "dd-MM-yyyy", "yyyy-MM-dd",
                    "dd MMM yyyy", "d MMM yyyy"
                )
                
                for (format in dateFormats) {
                    try {
                        val sdf = SimpleDateFormat(format, Locale.getDefault())
                        return sdf.parse(dateStr) ?: Date()
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
    private fun extractTotal(text: String): Double {
        // Common patterns for total amount
        val totalPatterns = listOf(
            Pattern.compile("(?i)total\\s*[:=]?\\s*(\\$?\\s*[\\d,]+\\.\\d{2})"),
            Pattern.compile("(?i)amount\\s*[:=]?\\s*(\\$?\\s*[\\d,]+\\.\\d{2})"),
            Pattern.compile("(?i)grand total\\s*[:=]?\\s*(\\$?\\s*[\\d,]+\\.\\d{2})"),
            Pattern.compile("(?i)sum\\s*[:=]?\\s*(\\$?\\s*[\\d,]+\\.\\d{2})"),
            Pattern.compile("(?i)to pay\\s*[:=]?\\s*(\\$?\\s*[\\d,]+\\.\\d{2})")
        )
        
        // Try each pattern
        for (pattern in totalPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val totalStr = matcher.group(1)
                    .replace("$", "")
                    .replace(",", "")
                    .replace(" ", "")
                
                try {
                    return totalStr.toDouble()
                } catch (e: NumberFormatException) {
                    // Try next pattern
                }
            }
        }
        
        // If no specific total found, look for last price in the receipt
        val pricePattern = Pattern.compile("(\\d+[,\\d]*\\.\\d{2})")
        val matcher = pricePattern.matcher(text)
        var lastPrice = 0.0
        
        while (matcher.find()) {
            try {
                val priceStr = matcher.group(1).replace(",", "")
                val price = priceStr.toDouble()
                lastPrice = price
            } catch (e: NumberFormatException) {
                // Skip invalid prices
            }
        }
        
        return lastPrice
    }
    
    /**
     * Extract line items from receipt text
     */
    private fun extractLineItems(text: String): List<Map<String, Any>> {
        val items = mutableListOf<Map<String, Any>>()
        val lines = text.split("\n")
        
        // Simple item pattern: item name followed by price
        val itemPattern = Pattern.compile("(.+?)\\s+(\\d+(?:\\.\\d{2})?)\\s*$")
        
        for (line in lines) {
            val matcher = itemPattern.matcher(line)
            if (matcher.find()) {
                try {
                    val name = matcher.group(1).trim()
                    val price = matcher.group(2).toDouble()
                    
                    // Skip very short item names or suspiciously high prices
                    if (name.length > 2 && price > 0 && price < 10000) {
                        val item = mapOf(
                            "name" to name,
                            "price" to price,
                            "quantity" to 1.0,
                            "total" to price
                        )
                        items.add(item)
                    }
                } catch (e: Exception) {
                    // Skip this line if parsing fails
                }
            }
        }
        
        return items
    }
    
    /**
     * Extract currency from receipt text
     */
    private fun extractCurrency(text: String): String {
        // Look for currency symbols or codes
        val currencyPatterns = mapOf(
            Pattern.compile("\\$") to "USD",
            Pattern.compile("€") to "EUR",
            Pattern.compile("£") to "GBP",
            Pattern.compile("¥") to "JPY",
            Pattern.compile("USD") to "USD",
            Pattern.compile("EUR") to "EUR",
            Pattern.compile("IDR") to "IDR",
            Pattern.compile("Rp") to "IDR"
        )
        
        for ((pattern, currency) in currencyPatterns) {
            if (pattern.matcher(text).find()) {
                return currency
            }
        }
        
        // Default to IDR for Indonesian receipts
        return "IDR"
    }
    
    /**
     * Extract additional notes from receipt text
     */
    private fun extractNotes(text: String): String {
        // Look for special notes, comments, or additional information
        val notesPatterns = listOf(
            Pattern.compile("(?i)note\\s*:(.+)"),
            Pattern.compile("(?i)comment\\s*:(.+)"),
            Pattern.compile("(?i)special instruction\\s*:(.+)")
        )
        
        for (pattern in notesPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return matcher.group(1).trim()
            }
        }
        
        return ""
    }
    
    /**
     * Categorize receipt based on merchant name and text
     */
    private fun categorizeReceipt(merchantName: String, text: String): String {
        // Simple categorization based on keywords
        val categories = mapOf(
            listOf("restaurant", "cafe", "coffee", "resto", "food", "bakery", "pizza", "burger") to "Food & Dining",
            listOf("market", "supermarket", "grocery", "mart", "minimarket") to "Groceries",
            listOf("transport", "taxi", "uber", "grab", "gojek", "train", "bus", "mrt") to "Transportation",
            listOf("pharmacy", "drug", "apotik", "clinic", "hospital", "doctor") to "Healthcare",
            listOf("mall", "shop", "store", "retail", "boutique", "clothing") to "Shopping",
            listOf("cinema", "movie", "theater", "entertainment", "concert") to "Entertainment",
            listOf("hotel", "motel", "airbnb", "accommodation", "hostel", "resort") to "Travel",
            listOf("bill", "utility", "electric", "water", "gas", "internet", "phone") to "Bills & Utilities"
        )
        
        val lcText = (merchantName + " " + text).toLowerCase()
        
        for ((keywords, category) in categories) {
            if (keywords.any { lcText.contains(it) }) {
                return category
            }
        }
        
        return "Uncategorized"
    }
}
