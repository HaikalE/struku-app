package com.example.struku.domain.model

import java.util.Date

/**
 * Model domain untuk anggaran (budget)
 */
data class Budget(
    val id: Int = 0,
    val categoryId: String,
    val amount: Double,
    val month: Int, // 1-12
    val year: Int,
    val createdAt: Date = Date()
)