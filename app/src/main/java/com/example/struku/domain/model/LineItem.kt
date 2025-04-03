package com.example.struku.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.text.NumberFormat
import java.util.Locale

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
    val notes: String = "",
    val unitPrice: Double? = null
) : Parcelable {
    
    /**
     * Item description (alias to name for compatibility)
     */
    val description: String get() = name
    
    /**
     * Calculate total price for this item
     */
    fun getTotal(): Double {
        return price * quantity
    }
    
    /**
     * Get unit price, calculated from total if not explicitly set
     */
    fun getUnitPrice(): Double {
        return unitPrice ?: (price / quantity)
    }
    
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
        return format.format(getTotal())
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
