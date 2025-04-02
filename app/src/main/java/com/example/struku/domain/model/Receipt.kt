package com.example.struku.domain.model

import java.util.Date

/**
 * Model domain untuk struk
 */
data class Receipt(
    val id: Int = 0,
    val merchantName: String,
    val date: Date,
    val total: Double,
    val currency: String = "IDR",
    val category: String,
    val imageUri: String? = null,
    val items: List<LineItem> = emptyList(),
    val notes: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)