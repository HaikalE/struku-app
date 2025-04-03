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
            val items = extractLineItems(text)
            val currency = extractCurrency(text)
            
            // Calculate total from extracted items
            val total = if (items.isNotEmpty()) {
                items.sumOf { (it["total"] as? Double) ?: 0.0 }
            } else {
                // Fallback to extracting total directly
                extractTotal(text)
            }
            
            // Log the results for debugging
            Log.d(TAG, "Parsed $merchantName with ${items.size} items, total: $total $currency")
            
            // Build result map
            val result = mutableMapOf<String, Any>(
                "merchantName" to merchantName,
                "date" to date,
                "total" to total,
                "items" to items,
                "currency" to currency
            )
            
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
            Pattern.compile("(\\d{1,2}\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{2,4})", Pattern.CASE_INSENSITIVE),
            // Format like "04/03/2018 11:23AM"
            Pattern.compile("(\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4})\\s+\\d{1,2}:\\d{2}"),
            // Format dengan Mon (Month abbreviation) seperti "Mon 05/04/2018 11:25AM"
            Pattern.compile("(?:Mon|Tue|Wed|Thu|Fri|Sat|Sun)\\s+(\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4})")
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
                    "dd MMM yyyy", "d MMM yyyy",
                    "MM/dd/yyyy", "M/d/yyyy"
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
     * Extract line items from receipt text - enhanced to better detect various formats
     */
    private fun extractLineItems(text: String): List<Map<String, Any>> {
        val items = mutableListOf<Map<String, Any>>()
        val lines = text.split("\n")
        
        // Multiple patterns to match different receipt formats
        val itemPatterns = listOf(
            // Pattern 1: "Item Description   $12.34"
            Pattern.compile("(.+?)\\s+(\\$?\\s*\\d+[,\\d]*\\.\\d{2})\\s*$"),
            
            // Pattern 2: "1 x Item @ $12.34"
            Pattern.compile("(\\d+)\\s*[xX]\\s*(.+?)\\s*@?\\s*(\\$?\\s*\\d+[,\\d]*\\.\\d{2})"),
            
            // Pattern 3: "Item.....$12.34" (dots or spaces as separator)
            Pattern.compile("(.+?)[.\\s]{3,}(\\$?\\s*\\d+[,\\d]*\\.\\d{2})"),
            
            // Pattern 4: "Item Description $12.34 $12.34" (unit price and total)
            Pattern.compile("(.+?)\\s+(\\$?\\s*\\d+[,\\d]*\\.\\d{2})\\s+(\\$?\\s*\\d+[,\\d]*\\.\\d{2})"),
            
            // Pattern 5: "1 Item $12.34" (quantity at beginning)
            Pattern.compile("^(\\d+)\\s+(.+?)\\s+(\\$?\\s*\\d+[,\\d]*\\.\\d{2})\\s*$"),
            
            // Pattern 6: "N. Item Description   $12.34" (numbered item dengan titik)
            Pattern.compile("^(\\d+)\\s*\\.\\s*(.+?)\\s+(\\$?\\s*\\d+[,\\d]*\\.\\d{2})\\s*$"),
            
            // Pattern 7: Special untuk format struk Amerika dengan item bernomor
            Pattern.compile("^\\s*(\\d+)\\s*\\.?\\s*([\\w\\s\\p{Punct}&&[^\\$]]+)\\s*(\\$?\\s*\\d+[,\\d]*\\.\\d{2})\\s*$")
        )

        // Pattern for detecting the start of table headers or separator lines
        val headerPattern = Pattern.compile("(item|description|qty|quantity|price|amount|total)", Pattern.CASE_INSENSITIVE)
        val separatorPattern = Pattern.compile("^[-_.=*]{5,}$")
        
        // Flag to indicate we're in the items section of receipt
        var inItemsSection = false
        
        // Special case untuk struk yang memiliki banyak item bernomor
        var hasNumberedItems = false
        var consecutiveNumberedLines = 0
        
        for (i in lines.indices) {
            val line = lines[i]
            
            // Skip empty lines
            if (line.trim().isEmpty()) continue
            
            // Look for consecutive numbered lines to identify numbered item format
            if (line.trim().matches(Regex("^\\s*\\d+\\..*"))) {
                consecutiveNumberedLines++
                if (consecutiveNumberedLines >= 2) {
                    hasNumberedItems = true
                    inItemsSection = true
                }
            } else {
                consecutiveNumberedLines = 0
            }
            
            // Check if this is a header line
            if (headerPattern.matcher(line).find()) {
                inItemsSection = true
                continue
            }
            
            // Skip separator lines
            if (separatorPattern.matcher(line.trim()).matches()) {
                continue
            }
            
            // Skip lines likely to be not items
            if (line.length < 5 || 
                line.contains("SUBTOTAL", ignoreCase = true) ||
                line.contains("TAX", ignoreCase = true) ||
                line.contains("TOTAL", ignoreCase = true)) {
                continue
            }
            
            // Try each pattern until we find a match
            var matched = false
            
            // Prioritize Pattern 6 & 7 for numbered item formats if we detected such a format
            val patternsToTry = if (hasNumberedItems) {
                listOf(itemPatterns[6], itemPatterns[5], itemPatterns[0], itemPatterns[1], 
                      itemPatterns[2], itemPatterns[3], itemPatterns[4])
            } else {
                itemPatterns
            }
            
            for (pattern in patternsToTry) {
                val matcher = pattern.matcher(line)
                if (matcher.find()) {
                    try {
                        when (pattern) {
                            itemPatterns[0] -> { // Pattern 1: Item Description $12.34
                                val name = matcher.group(1).trim()
                                val priceStr = matcher.group(2).replace("$", "").replace(",", "").trim()
                                val price = priceStr.toDouble()
                                
                                items.add(mapOf(
                                    "name" to name,
                                    "price" to price,
                                    "quantity" to 1.0,
                                    "total" to price
                                ))
                                matched = true
                            }
                            
                            itemPatterns[1] -> { // Pattern 2: 1 x Item @ $12.34
                                val quantity = matcher.group(1).toDouble()
                                val name = matcher.group(2).trim()
                                val priceStr = matcher.group(3).replace("$", "").replace(",", "").trim()
                                val price = priceStr.toDouble() 
                                
                                items.add(mapOf(
                                    "name" to name,
                                    "price" to price,
                                    "quantity" to quantity,
                                    "total" to (price * quantity)
                                ))
                                matched = true
                            }
                            
                            itemPatterns[2] -> { // Pattern 3: Item.....$12.34
                                val name = matcher.group(1).trim()
                                val priceStr = matcher.group(2).replace("$", "").replace(",", "").trim()
                                val price = priceStr.toDouble()
                                
                                items.add(mapOf(
                                    "name" to name,
                                    "price" to price,
                                    "quantity" to 1.0,
                                    "total" to price
                                ))
                                matched = true
                            }
                            
                            itemPatterns[3] -> { // Pattern 4: Item Description $12.34 $12.34
                                val name = matcher.group(1).trim()
                                val unitPriceStr = matcher.group(2).replace("$", "").replace(",", "").trim()
                                val totalPriceStr = matcher.group(3).replace("$", "").replace(",", "").trim()
                                
                                val unitPrice = unitPriceStr.toDouble()
                                val totalPrice = totalPriceStr.toDouble()
                                
                                // Calculate quantity based on total and unit price
                                val quantity = if (unitPrice > 0) totalPrice / unitPrice else 1.0
                                
                                items.add(mapOf(
                                    "name" to name,
                                    "price" to unitPrice,
                                    "quantity" to quantity,
                                    "total" to totalPrice
                                ))
                                matched = true
                            }
                            
                            itemPatterns[4] -> { // Pattern 5: 1 Item $12.34
                                val quantity = matcher.group(1).toDouble()
                                val name = matcher.group(2).trim()
                                val priceStr = matcher.group(3).replace("$", "").replace(",", "").trim()
                                val price = priceStr.toDouble() / quantity  // This is the unit price
                                
                                items.add(mapOf(
                                    "name" to name,
                                    "price" to price,
                                    "quantity" to quantity,
                                    "total" to price * quantity
                                ))
                                matched = true
                            }
                            
                            itemPatterns[5], itemPatterns[6] -> { // Pattern 6 & 7: N. Item Description $12.34
                                val itemNumber = matcher.group(1).toInt()
                                val name = matcher.group(2).trim()
                                val priceStr = matcher.group(3).replace("$", "").replace(",", "").trim()
                                val price = priceStr.toDouble()
                                
                                items.add(mapOf(
                                    "name" to name,
                                    "price" to price,
                                    "quantity" to 1.0,
                                    "total" to price,
                                    "itemNumber" to itemNumber
                                ))
                                matched = true
                                inItemsSection = true
                            }
                        }
                        
                        if (matched) break
                    } catch (e: Exception) {
                        // Skip this pattern if parsing fails
                        Log.d(TAG, "Failed to parse item with pattern: $e")
                    }
                }
            }
            
            // Try with a simple number extraction if no other matches found
            if (!matched && inItemsSection) {
                try {
                    // Last-ditch effort to find prices in the line
                    val priceFinderPattern = Pattern.compile("\\$(\\d+\\.\\d{2})")
                    val priceMatcher = priceFinderPattern.matcher(line)
                    
                    if (priceMatcher.find()) {
                        // Extract the price and assume the rest is description
                        val priceStr = priceMatcher.group(1)
                        val price = priceStr.toDouble()
                        
                        // Extract description by removing the price part
                        val description = line.substring(0, priceMatcher.start()).trim()
                        
                        if (description.isNotEmpty() && price > 0) {
                            items.add(mapOf(
                                "name" to description,
                                "price" to price,
                                "quantity" to 1.0,
                                "total" to price
                            ))
                        }
                    }
                } catch (e: Exception) {
                    // Just continue if this fails
                }
            }
        }
        
        // Jika tidak ada item yang ditemukan, coba dengan pendekatan yang lebih agresif
        if (items.isEmpty()) {
            Log.d(TAG, "No items found with standard patterns, trying aggressive approach")
            
            // Mencoba mencari bagian teks yang mengandung daftar item (biasanya bagian tengah struk)
            val potentialItemSection = text.split("\n")
                .drop(3) // Skip header
                .dropLastWhile { it.contains("TOTAL", ignoreCase = true) || it.isEmpty() }
                .joinToString("\n")
            
            // Mencari semua kemungkinan harga dalam format $xx.xx
            val pricePattern = Pattern.compile("\\$(\\d+\\.\\d{2})")
            val matcher = pricePattern.matcher(potentialItemSection)
            
            var lastIndex = 0
            var lastItemName = ""
            
            while (matcher.find()) {
                try {
                    val priceStr = matcher.group(1)
                    val price = priceStr.toDouble()
                    
                    // Ambil teks di antara harga sebelumnya dan harga ini sebagai nama item
                    val start = if (lastIndex == 0) 0 else lastIndex
                    val end = matcher.start()
                    
                    if (end > start) {
                        var itemName = potentialItemSection.substring(start, end).trim()
                        
                        // Bersihkan nama item dari angka di awal dan karakter khusus
                        itemName = itemName.replace(Regex("^\\d+\\.?\\s*"), "").trim()
                        
                        if (itemName.isNotEmpty() && itemName != lastItemName) {
                            items.add(mapOf(
                                "name" to itemName,
                                "price" to price,
                                "quantity" to 1.0,
                                "total" to price
                            ))
                            lastItemName = itemName
                        }
                    }
                    
                    lastIndex = matcher.end()
                } catch (e: Exception) {
                    // Skip if parsing fails
                }
            }
        }
        
        // Filter out suspicious items
        val filteredItems = items.filter { 
            val name = it["name"] as String
            val price = it["price"] as Double
            val quantity = it["quantity"] as Double
            
            name.isNotEmpty() && 
            name.length > 2 && 
            price > 0 && 
            price < 10000 &&  // Exclude very high prices
            quantity > 0 &&
            !name.matches(Regex("(?i)total|subtotal|tax|discount|service"))  // Skip headers or summary lines
        }
        
        Log.d(TAG, "Extracted ${filteredItems.size} items from receipt")
        return filteredItems
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
     * Extract total amount from receipt text (as a fallback)
     */
    private fun extractTotal(text: String): Double {
        // Common patterns for total amount
        val totalPatterns = listOf(
            Pattern.compile("(?i)total\\s*[:=]?\\s*(\\$?\\s*[\\d,]+\\.\\d{2})"),
            Pattern.compile("(?i)amount\\s*[:=]?\\s*(\\$?\\s*[\\d,]+\\.\\d{2})"),
            Pattern.compile("(?i)grand total\\s*[:=]?\\s*(\\$?\\s*[\\d,]+\\.\\d{2})"),
            Pattern.compile("(?i)sum\\s*[:=]?\\s*(\\$?\\s*[\\d,]+\\.\\d{2})"),
            Pattern.compile("(?i)to pay\\s*[:=]?\\s*(\\$?\\s*[\\d,]+\\.\\d{2})"),
            Pattern.compile("(?i)TOTAL\\s*:\\s*(\\$?\\s*[\\d,]+\\.\\d{2})"),
            Pattern.compile("TOTAL[^\\n]*?(\\$?\\s*[\\d,]+\\.\\d{2})")
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
        
        val lcText = (merchantName + " " + text).lowercase()
        
        for ((keywords, category) in categories) {
            if (keywords.any { lcText.contains(it) }) {
                return category
            }
        }
        
        return "Uncategorized"
    }
}