package com.example.struku.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    // TODO: Inject preferences repository
) : ViewModel() {
    
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsState()
        )
    
    init {
        // TODO: Load preferences from repository
        // For now, using default values
    }
    
    fun toggleBiometric() {
        viewModelScope.launch {
            _state.update { it.copy(useBiometric = !it.useBiometric) }
            // TODO: Save to preferences repository
        }
    }
    
    fun setCurrency(currency: String) {
        viewModelScope.launch {
            _state.update { it.copy(currency = currency) }
            // TODO: Save to preferences repository
        }
    }
    
    fun setLanguage(language: String) {
        viewModelScope.launch {
            _state.update { it.copy(language = language) }
            // TODO: Save to preferences repository
        }
    }
    
    fun setTheme(theme: String) {
        viewModelScope.launch {
            _state.update { it.copy(theme = theme) }
            // TODO: Save to preferences repository
        }
    }
}

data class SettingsState(
    val useBiometric: Boolean = false,
    val currency: String = "IDR",
    val language: String = "Bahasa Indonesia",
    val theme: String = "System"
)