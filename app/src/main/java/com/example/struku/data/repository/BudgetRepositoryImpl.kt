package com.example.struku.data.repository

import com.example.struku.data.local.dao.BudgetDao
import com.example.struku.data.mapper.toBudget
import com.example.struku.data.mapper.toBudgetEntity
import com.example.struku.domain.model.Budget
import com.example.struku.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override suspend fun saveBudget(budget: Budget) {
        budgetDao.insertBudget(budget.toBudgetEntity())
    }

    override suspend fun updateBudget(budget: Budget) {
        budgetDao.updateBudget(budget.toBudgetEntity())
    }

    override suspend fun deleteBudget(budgetId: Int) {
        val budget = budgetDao.getBudgetById(budgetId) ?: return
        budgetDao.deleteBudget(budget)
    }

    override fun getBudgetsByMonthYear(month: Int, year: Int): Flow<List<Budget>> {
        return budgetDao.getBudgetsByMonthYear(month, year).map { entities ->
            entities.map { it.toBudget() }
        }
    }

    override suspend fun getBudgetByCategoryAndPeriod(categoryId: String, month: Int, year: Int): Budget? {
        return budgetDao.getBudgetByCategoryAndPeriod(categoryId, month, year)?.toBudget()
    }
}
