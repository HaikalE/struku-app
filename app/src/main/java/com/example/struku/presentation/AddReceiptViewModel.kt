package com.example.struku.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.struku.domain.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

private const val TAG = "AddReceiptViewModel"

@HiltViewModel
class AddReceiptViewModel @Inject constructor(
    val repository: ReceiptRepository
) : ViewModel() {
    
    init {
        Log.d(TAG, "AddReceiptViewModel initialized")
    }
}
