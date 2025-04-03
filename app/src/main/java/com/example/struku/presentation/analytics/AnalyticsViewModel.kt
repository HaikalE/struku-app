package com.example.struku.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.struku.domain.model.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * ViewModel for the analytics screen
 */
@HiltViewModel
class AnalyticsViewModel @Inject constructor() : ViewModel() {
    
    private val _state = MutableStateFlow(AnalyticsState())
    val state: StateFlow<AnalyticsState> = _state.asStateFlow()
    
    init {
        // Initialize with current month and year
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based
        val currentYear = calendar.get(Calendar.YEAR)
        
        changeMonth(currentMonth, currentYear)
    }
    
    /**
     * Change the month for analytics data
     */
    fun changeMonth(month: Int, year: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                month = month,
                year = year
            )
            
            loadAnalyticsData(month, year)
        }
    }
    
    /**
     * Load analytics data for the specified month and year
     */
    private fun loadAnalyticsData(month: Int, year: Int) {
        viewModelScope.launch {
            try {
                // In a real app, this would come from a repository
                val expensesByCategory = generateSampleExpenseData()
                val budgetByCategory = generateSampleBudgetData()
                
                // Calculate percent of budget used for each category
                val percentByCategory = calculatePercentUsed(expensesByCategory, budgetByCategory)
                
                _state.value = _state.value.copy(
                    expensesByCategory = expensesByCategory,
                    budgetByCategory = budgetByCategory,
                    percentByCategory = percentByCategory,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    /**
     * Generate sample expense data
     */
    private fun generateSampleExpenseData(): Map<String, Double> {
        val expenseMap = mutableMapOf<String, Double>()
        
        // For each category, generate a random expense amount
        Category.DEFAULT_CATEGORIES.forEach { category ->
            if (Math.random() > 0.2) { // 80% chance to have expense in this category
                val expenseAmount = (20000..1500000).random().toDouble() // Between 20K and 1.5M IDR
                expenseMap[category.id] = expenseAmount
            }
        }
        
        return expenseMap
    }
    
    /**
     * Generate sample budget data
     */
    private fun generateSampleBudgetData(): Map<String, Double> {
        val budgetMap = mutableMapOf<String, Double>()
        
        // For each category, generate a random budget amount
        Category.DEFAULT_CATEGORIES.forEach { category ->
            if (Math.random() > 0.3) { // 70% chance to have budget in this category
                val budgetAmount = (100000..2000000).random().toDouble() // Between 100K and 2M IDR
                budgetMap[category.id] = budgetAmount
            }
        }
        
        return budgetMap
    }
    
    /**
     * Calculate percent of budget used for each category
     */
    private fun calculatePercentUsed(
        expenses: Map<String, Double>,
        budgets: Map<String, Double>
    ): Map<String, Double> {
        val percentMap = mutableMapOf<String, Double>()
        
        expenses.forEach { (categoryId, expense) ->
            val budget = budgets[categoryId]
            if (budget != null && budget > 0) {
                val percentUsed = (expense / budget) * 100
                percentMap[categoryId] = percentUsed
            }
        }
        
        return percentMap
    }
}

/**
 * State for the analytics screen
 */
data class AnalyticsState(
    val month: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val year: Int = Calendar.getInstance().get(Calendar.YEAR),
    val expensesByCategory: Map<String, Double> = emptyMap(),
    val budgetByCategory: Map<String, Double> = emptyMap(),
    val percentByCategory: Map<String, Double> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
)