package com.example.struku.domain.usecase

import com.example.struku.domain.model.Budget
import com.example.struku.domain.repository.BudgetRepository
import com.example.struku.domain.repository.ReceiptRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

/**
 * Use case untuk mendapatkan laporan pengeluaran bulanan
 */
class GetMonthlyReportUseCase @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val budgetRepository: BudgetRepository
) {
    /**
     * Mendapatkan laporan pengeluaran bulanan
     * @param month Bulan (1-12)
     * @param year Tahun
     * @return Map kategori ke total pengeluaran dan status anggaran
     */
    suspend operator fun invoke(month: Int, year: Int): Map<String, ExpenseReport> {
        val startDate = getStartOfMonth(month, year)
        val endDate = getEndOfMonth(month, year)
        
        // Dapatkan total pengeluaran per kategori
        val totalByCategory = receiptRepository.getTotalByCategory(startDate, endDate)
        
        // Dapatkan anggaran untuk bulan dan tahun yang ditentukan
        val budgets = budgetRepository.getBudgetsByMonthYear(month, year).first()
        
        // Buat laporan untuk setiap kategori
        return totalByCategory.mapValues { (category, total) ->
            val budget = budgets.find { it.categoryId == category }
            ExpenseReport(
                totalSpent = total,
                budgetAmount = budget?.amount,
                percentUsed = if (budget != null && budget.amount > 0) (total / budget.amount * 100) else null
            )
        }
    }
    
    /**
     * Helper untuk mendapatkan tanggal awal bulan
     */
    private fun getStartOfMonth(month: Int, year: Int): Date {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1) // Months are 0-indexed in Calendar
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }
    
    /**
     * Helper untuk mendapatkan tanggal akhir bulan
     */
    private fun getEndOfMonth(month: Int, year: Int): Date {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1) // Months are 0-indexed in Calendar
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time
    }
    
    /**
     * Data class untuk laporan pengeluaran per kategori
     */
    data class ExpenseReport(
        val totalSpent: Double,
        val budgetAmount: Double? = null,
        val percentUsed: Double? = null
    ) {
        val isOverBudget: Boolean
            get() = budgetAmount != null && totalSpent > budgetAmount
    }
}