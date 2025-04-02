package com.example.struku.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.struku.domain.usecase.GetMonthlyReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getMonthlyReportUseCase: GetMonthlyReportUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(AnalyticsState())
    val state: StateFlow<AnalyticsState> = _state
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AnalyticsState(isLoading = true)
        )
    
    init {
        loadCurrentMonthData()
    }
    
    private fun loadCurrentMonthData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val calendar = Calendar.getInstance()
                val currentMonth = calendar.get(Calendar.MONTH) + 1 // Months are 0-indexed in Calendar
                val currentYear = calendar.get(Calendar.YEAR)
                
                val monthlyReport = getMonthlyReportUseCase(currentMonth, currentYear)
                _state.update { state ->
                    state.copy(
                        isLoading = false,
                        expensesByCategory = monthlyReport.mapValues { it.value.totalSpent },
                        budgetByCategory = monthlyReport.mapValues { it.value.budgetAmount },
                        percentByCategory = monthlyReport.mapValues { it.value.percentUsed },
                        month = currentMonth,
                        year = currentYear
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error loading analytics"
                ) }
            }
        }
    }
    
    fun changeMonth(month: Int, year: Int) {
        if (month != _state.value.month || year != _state.value.year) {
            viewModelScope.launch {
                _state.update { it.copy(isLoading = true) }
                try {
                    val monthlyReport = getMonthlyReportUseCase(month, year)
                    _state.update { state ->
                        state.copy(
                            isLoading = false,
                            expensesByCategory = monthlyReport.mapValues { it.value.totalSpent },
                            budgetByCategory = monthlyReport.mapValues { it.value.budgetAmount },
                            percentByCategory = monthlyReport.mapValues { it.value.percentUsed },
                            month = month,
                            year = year
                        )
                    }
                } catch (e: Exception) {
                    _state.update { it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error loading analytics"
                    ) }
                }
            }
        }
    }
}

data class AnalyticsState(
    val isLoading: Boolean = false,
    val expensesByCategory: Map<String, Double> = emptyMap(),
    val budgetByCategory: Map<String, Double?> = emptyMap(),
    val percentByCategory: Map<String, Double?> = emptyMap(),
    val month: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val year: Int = Calendar.getInstance().get(Calendar.YEAR),
    val error: String? = null
)