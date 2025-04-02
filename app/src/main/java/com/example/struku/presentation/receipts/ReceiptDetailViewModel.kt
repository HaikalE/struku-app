package com.example.struku.presentation.receipts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.struku.domain.model.Receipt
import com.example.struku.domain.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiptDetailViewModel @Inject constructor(
    private val receiptRepository: ReceiptRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ReceiptDetailState())
    val state: StateFlow<ReceiptDetailState> = _state
    
    private var currentReceiptId: Long = 0L
    
    fun loadReceipt(receiptId: Long) {
        currentReceiptId = receiptId
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                val receipt = receiptRepository.getReceiptById(receiptId)
                _state.update { it.copy(isLoading = false, receipt = receipt) }
            } catch (e: Exception) {
                _state.update { it.copy(
                    isLoading = false,
                    error = "Tidak dapat memuat struk: ${e.message}"
                ) }
            }
        }
    }
    
    fun deleteReceipt() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                receiptRepository.deleteReceipt(currentReceiptId)
                _state.update { it.copy(isLoading = false, isDeleted = true) }
            } catch (e: Exception) {
                _state.update { it.copy(
                    isLoading = false,
                    error = "Gagal menghapus struk: ${e.message}"
                ) }
            }
        }
    }
}

data class ReceiptDetailState(
    val isLoading: Boolean = false,
    val receipt: Receipt? = null,
    val error: String? = null,
    val isDeleted: Boolean = false
)
