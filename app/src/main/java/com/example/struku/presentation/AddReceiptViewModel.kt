package com.example.struku.presentation

import androidx.lifecycle.ViewModel
import com.example.struku.domain.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for adding a new receipt
 */
@HiltViewModel
class AddReceiptViewModel @Inject constructor(
    val repository: ReceiptRepository
) : ViewModel()
