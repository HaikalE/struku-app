package com.example.struku.presentation.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.struku.domain.model.Category
import com.example.struku.domain.model.LineItem
import com.example.struku.domain.model.Receipt
import com.example.struku.domain.repository.CategoryRepository
import com.example.struku.domain.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ReceiptReviewState())
    val state: StateFlow<ReceiptReviewState> = _state
    
    init {
        loadCategories()
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
    
    fun loadReceipt(receiptId: Int) {
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
    
    fun updateTotal(total: Double) {
        _state.update { state ->
            val receipt = state.receipt ?: return@update state
            state.copy(receipt = receipt.copy(total = total))
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
                items[index] = items[index].copy(description = description)
                state.copy(receipt = receipt.copy(items = items))
            } else {
                state
            }
        }
    }
    
    fun updateItemQuantity(index: Int, quantity: Int) {
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
    }
    
    fun addLineItem() {
        _state.update { state ->
            val receipt = state.receipt ?: return@update state
            val items = receipt.items.toMutableList()
            items.add(LineItem(
                receiptId = receipt.id,
                description = "",
                quantity = 1,
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
    }
    
    fun saveReceipt() {
        viewModelScope.launch {
            val receipt = _state.value.receipt ?: return@launch
            
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                receiptRepository.updateReceipt(receipt)
                _state.update { it.copy(
                    isLoading = false,
                    saveCompleted = true
                ) }
            } catch (e: Exception) {
                _state.update { it.copy(
                    isLoading = false,
                    error = "Gagal menyimpan struk: ${e.message}"
                ) }
            }
        }
    }
}

data class ReceiptReviewState(
    val isLoading: Boolean = false,
    val receipt: Receipt? = null,
    val availableCategories: List<Category> = emptyList(),
    val error: String? = null,
    val saveCompleted: Boolean = false
)
