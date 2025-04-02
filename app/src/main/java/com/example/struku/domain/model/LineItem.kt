package com.example.struku.domain.model

/**
 * Model domain untuk item baris dalam struk
 */
data class LineItem(
    val id: Long = 0,
    val receiptId: Long = 0,
    val description: String,
    val quantity: Int = 1,
    val unitPrice: Double? = null,
    val price: Double,
    val category: String? = null
)