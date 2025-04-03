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
    val currency: String? = "IDR",
    val category: String = "",
    val imageUri: String? = null,
    val items: List<LineItem> = emptyList(),
    val notes: String = "",
    val createdAt: @RawValue Date? = null,
    val updatedAt: @RawValue Date? = null
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
}

/**
 * Line item domain model
 */
@Parcelize
data class LineItem(
    val id: Long = 0,
    val name: String,
    val price: Double,
    val quantity: Double = 1.0,
    val category: String = "",
    val notes: String = ""
) : Parcelable {
    
    /**
     * Formatted price
     */
    fun getFormattedPrice(): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return format.format(price)
    }
    
    /**
     * Formatted total
     */
    fun getFormattedTotal(): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return format.format(price * quantity)
    }
    
    /**
     * Formatted quantity
     */
    fun getFormattedQuantity(): String {
        return if (quantity == quantity.toInt().toDouble()) {
            quantity.toInt().toString()
        } else {
            String.format("%.2f", quantity)
        }
    }
}
