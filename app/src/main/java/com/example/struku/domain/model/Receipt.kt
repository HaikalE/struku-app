package com.example.struku.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Receipt entity for database storage
 */
@Entity(tableName = "receipts")
@Parcelize
data class Receipt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val merchantName: String,
    val date: Long,
    val total: Double,
    val category: String = "",
    val notes: String = "",
    val imagePath: String? = null,
    
    // These are populated by Room's relationship query
    @Transient
    val items: List<LineItem> = emptyList()
) : Parcelable {
    
    /**
     * Formatted date string
     */
    fun getFormattedDate(): String {
        val dateObj = Date(date)
        val format = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        return format.format(dateObj)
    }
    
    /**
     * Formatted total amount
     */
    fun getFormattedTotal(): String {
        val format = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
        return format.format(total)
    }
}

/**
 * Receipt with its line items (for Room relationship query)
 */
data class ReceiptWithItems(
    @Relation(
        parentColumn = "id",
        entityColumn = "receiptId"
    )
    val receipt: Receipt,
    val items: List<LineItem>
)

/**
 * Line item entity for database storage
 */
@Entity(tableName = "line_items")
@Parcelize
data class LineItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val receiptId: Long,
    val name: String,
    val price: Double,
    val quantity: Double = 1.0,
    val total: Double = price * quantity,
    val category: String = "",
    val notes: String = ""
) : Parcelable {
    
    /**
     * Formatted price
     */
    fun getFormattedPrice(): String {
        val format = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
        return format.format(price)
    }
    
    /**
     * Formatted total
     */
    fun getFormattedTotal(): String {
        val format = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
        return format.format(total)
    }
}