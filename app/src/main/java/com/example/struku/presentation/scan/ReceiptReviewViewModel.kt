package com.example.struku.presentation.scan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.struku.domain.model.Category
import com.example.struku.domain.model.LineItem
import com.example.struku.domain.model.Receipt
import com.example.struku.domain.repository.CategoryRepository
import com.example.struku.domain.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ReceiptReviewViewModel @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(ReceiptReviewState())
    val state: StateFlow<ReceiptReviewState> = _state
    
    // The receipt ID to be reviewed - use "receiptId" as the parameter name consistently
    private val receiptId: Long = savedStateHandle["receiptId"] ?: 0L
    
    init {
        loadCategories()
        
        // Load receipt if ID is provided
        if (receiptId > 0) {
            loadReceipt(receiptId)
        }
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val categories = categoryRepository.getAllCategories().first()
                _state.update { it.copy(availableCategories = categories) }
            } catch (e: Exception) {
                // If categories can't be loaded, use defaults
                _state.update { it.copy(
                    availableCategories = Category.DEFAULT_CATEGORIES,
                    error = "Could not load categories: ${e.message}"
                ) }
            }
        }
    }
    
    fun loadReceipt(receiptId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                val receipt = receiptRepository.getReceiptById(receiptId)
                _state.update { it.copy(isLoading = false, receipt = receipt) }
                
                // Calculate total automatically
                receipt?.let { calculateTotal() }
            } catch (e: Exception) {
                _state.update { it.copy(
                    isLoading = false,
                    error = "Tidak dapat memuat struk: ${e.message}"
                ) }
            }
        }
    }
    
    fun updateMerchantName(name: String) {
        _state.update { state ->
            val receipt = state.receipt ?: return@update state
            state.copy(receipt = receipt.copy(merchantName = name))
        }
    }
    
    fun updateDate(date: Date) {
        _state.update { state ->
            val receipt = state.receipt ?: return@update state
            state.copy(receipt = receipt.copy(date = date))
        }
    }
    
    // Disable manual total update - now calculated automatically
    private fun updateTotalInternal(total: Double) {
        _state.update { state ->
            val receipt = state.receipt ?: return@update state
            state.copy(receipt = receipt.copy(total = total))
        }
    }
    
    fun calculateTotal() {
        _state.value.receipt?.let { receipt ->
            val newTotal = receipt.items.sumOf { item -> item.quantity * item.price }
            updateTotalInternal(newTotal)
        }
    }
    
    fun updateCategory(category: String) {
        _state.update { state ->
            val receipt = state.receipt ?: return@update state
            state.copy(receipt = receipt.copy(category = category))
        }
    }
    
    fun updateItemDescription(index: Int, description: String) {
        _state.update { state ->
            val receipt = state.receipt ?: return@update state
            val items = receipt.items.toMutableList()
            if (index < items.size) {
                // Create a new copy of the LineItem with the name updated
                // (description is a property that maps to name)
                items[index] = items[index].copy(name = description)
                state.copy(receipt = receipt.copy(items = items))
            } else {
                state
            }
        }
    }
    
    fun updateItemQuantity(index: Int, quantity: Double) {
        _state.update { state ->
            val receipt = state.receipt ?: return@update state
            val items = receipt.items.toMutableList()
            if (index < items.size) {
                items[index] = items[index].copy(quantity = quantity)
                state.copy(receipt = receipt.copy(items = items))
            } else {
                state
            }
        }
        // Recalculate total when quantity changes
        calculateTotal()
    }
    
    fun updateItemPrice(index: Int, price: Double) {
        _state.update { state ->
            val receipt = state.receipt ?: return@update state
            val items = receipt.items.toMutableList()
            if (index < items.size) {
                items[index] = items[index].copy(price = price)
                state.copy(receipt = receipt.copy(items = items))
            } else {
                state
            }
        }
        // Recalculate total when price changes
        calculateTotal()
    }
    
    fun addLineItem() {
        _state.update { state ->
            val receipt = state.receipt ?: return@update state
            val items = receipt.items.toMutableList()
            items.add(LineItem(
                // Fixed: removed receiptId parameter that doesn't exist in LineItem constructor
                name = "",
                quantity = 1.0,
                price = 0.0
            ))
            state.copy(receipt = receipt.copy(items = items))
        }
    }
    
    fun removeLineItem(index: Int) {
        _state.update { state ->
            val receipt = state.receipt ?: return@update state
            val items = receipt.items.toMutableList()
            if (index < items.size) {
                items.removeAt(index)
                state.copy(receipt = receipt.copy(items = items))
            } else {
                state
            }
        }
        // Recalculate total when an item is removed
        calculateTotal()
    }
    
    fun saveReceipt() {
        viewModelScope.launch {
            val receipt = _state.value.receipt ?: return@launch
            
            // Calculate the total one final time before saving
            calculateTotal()
            
            _state.update { it.copy(isLoading = true, error = null, isSaving = true) }
            
            try {
                // Tunggu sebentar untuk memberikan visual feedback
                delay(500)
                
                // Dapatkan receipt yang sudah dihitung totalnya dari state terbaru
                val updatedReceipt = _state.value.receipt ?: return@launch
                
                // Lakukan penyimpanan dengan total yang sudah dihitung
                receiptRepository.updateReceipt(updatedReceipt)
                
                _state.update { it.copy(
                    isLoading = false,
                    isSaving = false,
                    saveCompleted = true,
                    saveMessage = "Struk berhasil disimpan",
                    savedReceiptId = updatedReceipt.id
                ) }
            } catch (e: Exception) {
                _state.update { it.copy(
                    isLoading = false,
                    isSaving = false,
                    error = "Gagal menyimpan struk: ${e.message}"
                ) }
            }
        }
    }
}

data class ReceiptReviewState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val receipt: Receipt? = null,
    val availableCategories: List<Category> = emptyList(),
    val error: String? = null,
    val saveCompleted: Boolean = false,
    val saveMessage: String? = null,
    val savedReceiptId: Long? = null
)
