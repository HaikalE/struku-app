package com.example.struku.domain.model

/**
 * Model domain untuk item baris dalam struk
 */
data class LineItem(
    val id: Int = 0,
    val receiptId: Int = 0,
    val description: String,
    val quantity: Int = 1,
    val unitPrice: Double? = null,
    val price: Double,
    val category: String? = null
)