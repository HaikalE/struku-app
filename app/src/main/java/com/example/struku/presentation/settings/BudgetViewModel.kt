package com.example.struku.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.struku.domain.model.Budget
import com.example.struku.domain.model.Category
import com.example.struku.domain.repository.BudgetRepository
import com.example.struku.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BudgetUiState(isLoading = true)
        )
    
    init {
        loadCategories()
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                categoryRepository.getAllCategories().collect { categories ->
                    _uiState.update { it.copy(availableCategories = categories) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Error loading categories: ${e.message}")
                }
            }
        }
    }
    
    fun getCurrentMonthBudgets() {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1 // Months are 0-indexed in Calendar
        val currentYear = calendar.get(Calendar.YEAR)
        
        getBudgets(currentMonth, currentYear)
    }
    
    fun getBudgets(month: Int, year: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, month = month, year = year) }
            
            try {
                val budgets = budgetRepository.getBudgetsByMonthYear(month, year).first()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        budgets = budgets,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error loading budgets: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun addBudget(categoryId: String, amount: Double, month: Int, year: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val newBudget = Budget(
                    categoryId = categoryId,
                    amount = amount,
                    month = month,
                    year = year,
                    createdAt = Date()
                )
                
                budgetRepository.saveBudget(newBudget)
                getBudgets(month, year) // Refresh list
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error adding budget: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun updateBudget(categoryId: String, amount: Double) {
        val selectedBudget = _uiState.value.selectedBudget ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val updatedBudget = selectedBudget.copy(
                    categoryId = categoryId,
                    amount = amount
                )
                
                budgetRepository.updateBudget(updatedBudget)
                getBudgets(_uiState.value.month, _uiState.value.year) // Refresh list
                _uiState.update { it.copy(selectedBudget = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error updating budget: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun deleteBudget(budgetId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                budgetRepository.deleteBudget(budgetId)
                getBudgets(_uiState.value.month, _uiState.value.year) // Refresh list
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Error deleting budget: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun selectBudgetForEdit(budget: Budget) {
        _uiState.update { it.copy(selectedBudget = budget) }
    }
    
    fun clearSelectedBudget() {
        _uiState.update { it.copy(selectedBudget = null) }
    }
}

data class BudgetUiState(
    val isLoading: Boolean = false,
    val budgets: List<Budget> = emptyList(),
    val availableCategories: List<Category> = emptyList(),
    val selectedBudget: Budget? = null,
    val month: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val year: Int = Calendar.getInstance().get(Calendar.YEAR),
    val error: String? = null
)