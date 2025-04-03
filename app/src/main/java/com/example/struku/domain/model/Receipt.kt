package com.example.struku.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Receipt domain model
 */
@Parcelize
data class Receipt(
    val id: Long = 0,
    val merchantName: String,
    val date: @RawValue Date,
    val total: Double,
    val currency: String = "IDR",
    val category: String = "",
    val imageUri: String? = null,
    val items: List<LineItem> = emptyList(),
    val notes: String = "",
    val createdAt: @RawValue Date = Date(),
    val updatedAt: @RawValue Date = Date()
) : Parcelable {
    
    /**
     * Formatted date string
     */
    fun getFormattedDate(): String {
        val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return format.format(date)
    }
    
    /**
     * Formatted total amount
     */
    fun getFormattedTotal(): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return format.format(total)
    }
    
    /**
     * Check if receipt has detailed line items
     */
    fun hasItems(): Boolean {
        return items.isNotEmpty()
    }
    
    /**
     * Get total amount calculated from line items
     */
    fun getCalculatedTotal(): Double {
        return items.sumOf { it.price * it.quantity }
    }
    
    /**
     * Check if receipt has a consistency issue between stated total and calculated total
     */
    fun hasTotalDiscrepancy(): Boolean {
        if (!hasItems()) return false
        
        val calculatedTotal = getCalculatedTotal()
        val difference = Math.abs(calculatedTotal - total)
        
        // Allow a small difference (0.5% of total)
        val tolerance = total * 0.005
        
        return difference > tolerance
    }
    
    /**
     * Get time since receipt was created
     */
    fun getTimeSinceCreated(): String {
        val now = Date()
        val diffInMillis = now.time - createdAt.time
        val diffInSeconds = diffInMillis / 1000
        val diffInMinutes = diffInSeconds / 60
        val diffInHours = diffInMinutes / 60
        val diffInDays = diffInHours / 24
        
        return when {
            diffInDays > 30 -> "${diffInDays / 30} month(s) ago"
            diffInDays > 0 -> "$diffInDays day(s) ago"
            diffInHours > 0 -> "$diffInHours hour(s) ago"
            diffInMinutes > 0 -> "$diffInMinutes minute(s) ago"
            else -> "Just now"
        }
    }
}
