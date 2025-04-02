package com.example.struku.presentation.scan

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.struku.domain.usecase.ScanReceiptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val scanReceiptUseCase: ScanReceiptUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow(ScannerState())
    val state: StateFlow<ScannerState> = _state
    
    fun scanReceipt(bitmap: Bitmap) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                val receiptId = scanReceiptUseCase.scanAndSave(bitmap)
                if (receiptId != null) {
                    _state.update { it.copy(
                        isLoading = false,
                        scannedReceiptId = receiptId.toInt()
                    ) }
                } else {
                    _state.update { it.copy(
                        isLoading = false,
                        error = "Tidak dapat mengenali struk. Silakan coba lagi."
                    ) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "Kesalahan tak terduga. Silakan coba lagi."
                ) }
            }
        }
    }
    
    fun resetScannedReceiptId() {
        _state.update { it.copy(scannedReceiptId = null) }
    }
}

data class ScannerState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val scannedReceiptId: Int? = null
)
