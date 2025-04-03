package com.example.struku.domain.usecase

import com.example.struku.domain.model.Receipt
import com.example.struku.domain.repository.CategoryRepository
import javax.inject.Inject

/**
 * Use case untuk mengategorikan pengeluaran secara otomatis
 */
class CategorizeExpenseUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    /**
     * Memprediksi kategori dari struk berdasarkan nama pedagang dan item
     */
    suspend operator fun invoke(receipt: Receipt): String {
        val itemNames = receipt.items.map { it.name }
        return categoryRepository.predictCategory(receipt.merchantName, itemNames)
    }
}