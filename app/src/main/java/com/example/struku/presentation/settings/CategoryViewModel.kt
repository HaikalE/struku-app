package com.example.struku.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.struku.domain.model.Category
import com.example.struku.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CategoryUiState(isLoading = true)
        )
    
    fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                categoryRepository.getAllCategories().collect { categories ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            categories = categories,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error loading categories"
                    )
                }
            }
        }
    }
    
    fun addCategory(name: String, color: Long, iconName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val categoryId = "custom_${UUID.randomUUID().toString().substring(0, 8)}"
                val newCategory = Category(
                    id = categoryId,
                    name = name,
                    color = color,
                    iconName = iconName,
                    isDefault = false,
                    isUserDefined = true
                )
                
                categoryRepository.saveCategory(newCategory)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error adding category"
                    )
                }
            }
        }
    }
    
    fun updateCategory(name: String, color: Long, iconName: String) {
        val selectedCategory = _uiState.value.selectedCategory ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val updatedCategory = selectedCategory.copy(
                    name = name,
                    color = color,
                    iconName = iconName
                )
                
                categoryRepository.updateCategory(updatedCategory)
                _uiState.update { it.copy(isLoading = false, selectedCategory = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error updating category"
                    )
                }
            }
        }
    }
    
    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                categoryRepository.deleteCategory(categoryId)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error deleting category"
                    )
                }
            }
        }
    }
    
    fun selectCategoryForEdit(category: Category) {
        _uiState.update { it.copy(selectedCategory = category) }
    }
    
    fun clearSelectedCategory() {
        _uiState.update { it.copy(selectedCategory = null) }
    }
}

data class CategoryUiState(
    val isLoading: Boolean = false,
    val categories: List<Category> = emptyList(),
    val selectedCategory: Category? = null,
    val error: String? = null
)