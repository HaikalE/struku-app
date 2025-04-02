package com.example.struku.domain.repository

import com.example.struku.domain.model.Budget
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface untuk operasi terkait anggaran (budget)
 */
interface BudgetRepository {
    /**
     * Menyimpan anggaran baru
     */
    suspend fun saveBudget(budget: Budget)
    
    /**
     * Memperbarui anggaran yang ada
     */
    suspend fun updateBudget(budget: Budget)
    
    /**
     * Menghapus anggaran
     */
    suspend fun deleteBudget(budgetId: Int)
    
    /**
     * Mendapatkan anggaran berdasarkan bulan dan tahun
     */
    fun getBudgetsByMonthYear(month: Int, year: Int): Flow<List<Budget>>
    
    /**
     * Mendapatkan anggaran berdasarkan kategori, bulan, dan tahun
     */
    suspend fun getBudgetByCategoryAndPeriod(categoryId: String, month: Int, year: Int): Budget?
}