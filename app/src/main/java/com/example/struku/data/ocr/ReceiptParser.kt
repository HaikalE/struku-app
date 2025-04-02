package com.example.struku.data.ocr

import com.example.struku.domain.model.LineItem
import com.example.struku.domain.model.Receipt
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Parser untuk mengekstrak data terstruktur dari teks struk
 */
class ReceiptParser @Inject constructor() {
    
    // Format tanggal yang umum di Indonesia
    private val dateFormats = listOf(
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()),
        SimpleDateFormat("dd MMM yyyy", Locale("id")),
        SimpleDateFormat("d MMM yyyy", Locale("id")),
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    )
    
    // Format mata uang
    private val currencyFormats = listOf(
        NumberFormat.getInstance(Locale("id")),
        NumberFormat.getInstance(Locale.US)
    )
    
    /**
     * Parse teks OCR menjadi objek Receipt
     */
    fun parseReceiptText(text: String): Receipt? {
        if (text.isBlank()) return null
        
        // Split into lines for analysis
        val lines = text.split("\n").filter { it.isNotBlank() }
        if (lines.isEmpty()) return null
        
        // Extract merchant name (typically in the first few lines)
        val merchantName = extractMerchantName(lines)
        
        // Extract date
        val date = extractDate(lines) ?: Date()
        
        // Extract total amount
        val (total, totalLine) = extractTotal(lines)
        
        // Extract items (usually lines between merchant name and total)
        val lineItems = extractLineItems(lines, merchantName, totalLine)
        
        // Default category
        val category = "other"
        
        return Receipt(
            merchantName = merchantName,
            date = date,
            total = total,
            currency = "IDR", // Default to IDR for Indonesia
            category = category,
            items = lineItems
        )
    }
    
    /**
     * Extract merchant name from the first few lines
     */
    private fun extractMerchantName(lines: List<String>): String {
        // For simplicity, we'll take the first line as the merchant name
        // In a real application, you might need more sophisticated logic
        return if (lines.isNotEmpty()) {
            lines[0].trim()
        } else {
            "Unknown Merchant"
        }
    }
    
    /**
     * Extract date from lines
     */
    private fun extractDate(lines: List<String>): Date? {
        val datePatterns = arrayOf(
            Pattern.compile("\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4}"),  // dd/mm/yyyy or variations
            Pattern.compile("\\d{1,2}\\s+[A-Za-z]{3,}\\s+\\d{2,4}"),  // dd MMM yyyy
            Pattern.compile("\\d{4}[/.-]\\d{1,2}[/.-]\\d{1,2}")   // yyyy-mm-dd
        )
        
        for (line in lines) {
            // Check for date patterns
            for (pattern in datePatterns) {
                val matcher = pattern.matcher(line)
                if (matcher.find()) {
                    val dateStr = matcher.group()
                    
                    // Try parsing with different date formats
                    for (format in dateFormats) {
                        try {
                            return format.parse(dateStr)
                        } catch (e: ParseException) {
                            // Try next format
                        }
                    }
                }
            }
            
            // Look for specific date keywords
            if (line.contains("Tanggal", ignoreCase = true) || 
                line.contains("Date", ignoreCase = true) ||
                line.contains("Tgl", ignoreCase = true)) {
                
                // Extract part after colon or similar separator
                val parts = line.split(":", "\\s+").map { it.trim() }
                if (parts.size > 1) {
                    for (i in 1 until parts.size) {
                        for (format in dateFormats) {
                            try {
                                return format.parse(parts[i])
                            } catch (e: ParseException) {
                                // Try next part or format
                            }
                        }
                    }
                }
            }
        }
        
        return null  // Date not found
    }
    
    /**
     * Extract total amount
     * @return Pair of (total amount, line index where total was found)
     */
    private fun extractTotal(lines: List<String>): Pair<Double, Int> {
        val totalKeywords = listOf(
            "total", "jumlah", "grand total", "amount", "total bayar", 
            "dibayar", "dibayarkan", "payment", "to pay"
        )
        
        // First, look for lines with total keywords
        for ((index, line) in lines.withIndex().reversed()) {
            val lowerLine = line.lowercase(Locale.getDefault())
            
            if (totalKeywords.any { lowerLine.contains(it) }) {
                // Extract numbers from this line
                val amount = extractAmount(line)
                if (amount > 0) {
                    return Pair(amount, index)
                }
            }
        }
        
        // If no explicit total found, try to find the largest amount in the bottom part of the receipt
        // (usually the last 30% of lines)
        val startIndex = (lines.size * 0.7).toInt()
        var maxAmount = 0.0
        var maxAmountIndex = -1
        
        for (i in startIndex until lines.size) {
            val amount = extractAmount(lines[i])
            if (amount > maxAmount) {
                maxAmount = amount
                maxAmountIndex = i
            }
        }
        
        return if (maxAmountIndex >= 0) {
            Pair(maxAmount, maxAmountIndex)
        } else {
            // Fallback: return 0 if no amount found
            Pair(0.0, lines.size - 1)
        }
    }
    
    /**
     * Extract amount from text
     */
    private fun extractAmount(text: String): Double {
        // Remove currency symbols and separators
        var cleanText = text.replace("[^0-9,.]\\s".toRegex(), " ")
        
        // Find patterns that look like money amounts
        val amountPatterns = arrayOf(
            // Rp notation with thousands separators
            Pattern.compile("Rp\\.?\\s*\\d{1,3}(\\.\\d{3})+([,\\.]\\d{1,2})?"),
            
            // Numbers with thousands separators
            Pattern.compile("\\d{1,3}(\\.\\d{3})+([,\\.]\\d{1,2})?"),
            
            // Numbers with decimal places
            Pattern.compile("\\d+[,\\.]\\d{1,2}")
        )
        
        for (pattern in amountPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val amountStr = matcher.group()
                    .replace("Rp", "")
                    .replace("Rp.", "")
                    .trim()
                
                // Try to parse with different formats
                for (format in currencyFormats) {
                    try {
                        return format.parse(amountStr).toDouble()
                    } catch (e: ParseException) {
                        // Try next format
                    }
                }
                
                // If format parsing fails, try manual parsing
                try {
                    // Replace thousand separator and fix decimal separator
                    val normalizedAmount = amountStr
                        .replace(".", "")
                        .replace(",", ".")
                    return normalizedAmount.toDouble()
                } catch (e: NumberFormatException) {
                    // Continue to next pattern
                }
            }
        }
        
        // If we get here, try to find any number in the text
        val numberPattern = Pattern.compile("\\d+([,\\.]\\d+)?")
        val matcher = numberPattern.matcher(text)
        if (matcher.find()) {
            try {
                val amountStr = matcher.group()
                    .replace(".", "")
                    .replace(",", ".")
                return amountStr.toDouble()
            } catch (e: NumberFormatException) {
                // Fall through to return 0
            }
        }
        
        return 0.0  // No valid amount found
    }
    
    /**
     * Extract line items from the receipt text
     */
    private fun extractLineItems(lines: List<String>, merchantName: String, totalLine: Int): List<LineItem> {
        val items = mutableListOf<LineItem>()
        
        // Define the lines that likely contain item entries
        // Skip the first few lines (usually header) and the last few lines (total, footer)
        val startLine = 3  // Skip header lines
        val endLine = if (totalLine > 0) totalLine else lines.size - 3
        
        if (startLine >= endLine) return items  // Not enough lines
        
        // Extract items from the middle section
        for (i in startLine until endLine) {
            val line = lines[i].trim()
            if (line.isBlank()) continue
            
            // Skip lines that are likely not items (e.g., section headers)
            if (line.length < 5) continue
            
            // Pattern 1: Item with quantity and price: "Item Name   2 x 10,000   20,000"
            val quantityPattern = Pattern.compile("(.+)\\s+(\\d+)\\s*x\\s*([\\d,.]+)\\s+([\\d,.]+)") 
            var matcher = quantityPattern.matcher(line)
            if (matcher.find()) {
                val description = matcher.group(1).trim()
                val quantity = matcher.group(2).toIntOrNull() ?: 1
                val unitPrice = extractAmount(matcher.group(3))
                val price = extractAmount(matcher.group(4))
                
                items.add(LineItem(
                    description = description,
                    quantity = quantity,
                    unitPrice = unitPrice,
                    price = price
                ))
                continue
            }
            
            // Pattern 2: Item with just price: "Item Name   10,000"
            val simplePattern = Pattern.compile("(.+)\\s+([\\d,.]+)$")
            matcher = simplePattern.matcher(line)
            if (matcher.find()) {
                val description = matcher.group(1).trim()
                val price = extractAmount(matcher.group(2))
                
                // Skip if the price is 0 or the description is suspicious
                if (price > 0 && description.length > 1 && 
                    !description.equals(merchantName, ignoreCase = true)) {
                    
                    items.add(LineItem(
                        description = description,
                        quantity = 1,
                        unitPrice = price,
                        price = price
                    ))
                }
            }
        }
        
        return items
    }
}
