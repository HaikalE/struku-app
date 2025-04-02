package com.example.struku.presentation.receipts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.struku.domain.model.Receipt
import com.example.struku.domain.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiptsViewModel @Inject constructor(
    private val receiptRepository: ReceiptRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(ReceiptsState())
    val state: StateFlow<ReceiptsState> = _state
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReceiptsState(isLoading = true)
        )
    
    init {
        loadReceipts()
    }
    
    private fun loadReceipts() {
        viewModelScope.launch {
            receiptRepository.getAllReceipts()
                .catch { error ->
                    _state.update { it.copy(
                        isLoading = false,
                        error = error.message ?: "Unknown error"
                    ) }
                }
                .collect { receipts ->
                    _state.update { it.copy(
                        isLoading = false,
                        receipts = receipts
                    ) }
                }
        }
    }
    
    fun refreshReceipts() {
        _state.update { it.copy(isLoading = true) }
        loadReceipts()
    }
}

data class ReceiptsState(
    val isLoading: Boolean = false,
    val receipts: List<Receipt> = emptyList(),
    val error: String? = null
)