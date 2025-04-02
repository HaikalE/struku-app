package com.example.struku.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.struku.domain.repository.ReceiptRepository
import com.example.struku.util.DataExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val dataExporter: DataExporter
) : ViewModel() {

    private val _state = MutableStateFlow(ExportState())
    val state: StateFlow<ExportState> = _state
    
    init {
        loadReceiptCount()
    }
    
    private fun loadReceiptCount() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                val receipts = receiptRepository.getAllReceipts().first()
                _state.update { it.copy(
                    isLoading = false,
                    receiptCount = receipts.size
                ) }
            } catch (e: Exception) {
                _state.update { it.copy(
                    isLoading = false,
                    error = "Gagal memuat data: ${e.message}"
                ) }
            }
        }
    }
    
    fun exportToCsv() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, message = null) }
            
            try {
                val receipts = receiptRepository.getAllReceipts().first()
                if (receipts.isEmpty()) {
                    _state.update { it.copy(
                        isLoading = false,
                        message = "Tidak ada data untuk diekspor"
                    ) }
                    return@launch
                }
                
                val uri = dataExporter.exportReceiptsToCsv(receipts)
                if (uri != null) {
                    dataExporter.shareExportedFile(uri, "text/csv")
                    _state.update { it.copy(
                        isLoading = false,
                        message = "Data berhasil diekspor!"
                    ) }
                } else {
                    _state.update { it.copy(
                        isLoading = false,
                        error = "Gagal mengekspor data"
                    ) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(
                    isLoading = false,
                    error = "Gagal mengekspor data: ${e.message}"
                ) }
            }
        }
    }
}

data class ExportState(
    val isLoading: Boolean = false,
    val receiptCount: Int = 0,
    val error: String? = null,
    val message: String? = null
)
