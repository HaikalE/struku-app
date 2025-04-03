package com.example.struku.presentation.debug

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.struku.data.ocr.AdvancedImagePreprocessor
import com.example.struku.data.ocr.PreprocessingVisualizer
import com.example.struku.data.ocr.ReceiptPreprocessingConfig
import com.example.struku.data.ocr.ReceiptPreprocessingConfigFactory
import com.example.struku.data.ocr.ReceiptType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for the debugging screen for OCR preprocessing
 */
@HiltViewModel
class PreprocessingDebugViewModel @Inject constructor(
    private val imagePreprocessor: AdvancedImagePreprocessor,
    private val preprocessingVisualizer: PreprocessingVisualizer
) : ViewModel() {
    
    private val TAG = "PreprocessingDebugVM"
    
    // Processing status
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()
    
    // Debug mode state
    private val _isDebugMode = MutableStateFlow(true)
    val isDebugMode = _isDebugMode.asStateFlow()
    
    // Last processed image
    private val _processedImage = MutableStateFlow<Bitmap?>(null)
    val processedImage = _processedImage.asStateFlow()
    
    // Get processing steps from the visualizer
    val processingSteps = preprocessingVisualizer.processingSteps
    
    // Currently selected config
    private val _selectedConfigType = MutableStateFlow(ReceiptType.AUTO_DETECT)
    val selectedConfigType = _selectedConfigType.asStateFlow()
    
    // Available configs for selection
    val availableConfigs = listOf(
        ReceiptType.AUTO_DETECT,
        ReceiptType.THERMAL,
        ReceiptType.INKJET,
        ReceiptType.LASER,
        ReceiptType.DIGITAL
    )
    
    init {
        // Activate debug mode by default
        setDebugMode(true)
        
        // Monitor preprocessing state
        viewModelScope.launch {
            imagePreprocessor.preprocessingState.collectLatest { state ->
                when (state) {
                    is AdvancedImagePreprocessor.PreprocessingState.InProgress -> {
                        _isProcessing.value = true
                    }
                    is AdvancedImagePreprocessor.PreprocessingState.Success -> {
                        _processedImage.value = state.resultBitmap
                        _isProcessing.value = false
                    }
                    is AdvancedImagePreprocessor.PreprocessingState.Error -> {
                        _isProcessing.value = false
                        Log.e(TAG, "Preprocessing error: ${state.message}", state.exception)
                    }
                    else -> { /* Do nothing for Idle state */ }
                }
            }
        }
    }
    
    /**
     * Enable or disable debug mode
     */
    fun setDebugMode(enabled: Boolean) {
        _isDebugMode.value = enabled
        preprocessingVisualizer.setDebugMode(enabled)
    }
    
    /**
     * Reset the debug session
     */
    fun resetSession() {
        preprocessingVisualizer.startNewSession()
    }
    
    /**
     * Set the receipt type for preprocessing
     */
    fun setReceiptType(receiptType: ReceiptType) {
        _selectedConfigType.value = receiptType
    }
    
    /**
     * Process an image with the selected preprocessing configuration
     */
    fun processImage(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                
                // Get config based on selected type
                val config = getConfigForType(_selectedConfigType.value)
                
                withContext(Dispatchers.Default) {
                    imagePreprocessor.processReceiptImage(bitmap, config)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image: ${e.message}", e)
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * Process multiple configurations for comparison
     */
    fun processWithAllConfigs(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                
                // Create a special multi-pass title
                preprocessingVisualizer.captureProcessingStep(
                    "Multi-pass Processing",
                    "Processing with different receipt type configurations for comparison",
                    bitmap
                )
                
                // Process with all config types
                withContext(Dispatchers.Default) {
                    availableConfigs.forEach { receiptType ->
                        if (receiptType != ReceiptType.AUTO_DETECT) {
                            val config = getConfigForType(receiptType)
                            
                            val processedImage = imagePreprocessor.processReceiptImage(bitmap, config)
                            
                            // Create comparison image
                            val comparisonImage = preprocessingVisualizer.generateBeforeAfterComparison(
                                bitmap,
                                processedImage,
                                "Receipt Type: $receiptType"
                            )
                            
                            // Capture the comparison
                            preprocessingVisualizer.captureProcessingStep(
                                "Comparison: $receiptType",
                                "Preprocessing optimized for receipt type: $receiptType",
                                comparisonImage
                            )
                        }
                    }
                }
                
                // Generate summary image
                val summaryImage = preprocessingVisualizer.generateProcessingSummary()
                if (summaryImage != null) {
                    preprocessingVisualizer.captureProcessingStep(
                        "Comparison Summary",
                        "Summary of all preprocessing configurations",
                        summaryImage
                    )
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in comparison processing: ${e.message}", e)
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * Process an image from a URI
     */
    fun processImageFromUri(uri: Uri, contentResolver: android.content.ContentResolver) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                
                val bitmap = withContext(Dispatchers.IO) {
                    android.provider.MediaStore.Images.Media.getBitmap(contentResolver, uri)
                }
                
                processImage(bitmap)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading image from URI: ${e.message}", e)
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * Save a processed step image to device storage
     */
    fun saveStepImage(step: PreprocessingVisualizer.ProcessingStep): File? {
        return preprocessingVisualizer.saveVisualizationToGallery(
            step.imageBitmap,
            "step_${step.id}_${step.name.replace(" ", "_")}"
        )
    }
    
    /**
     * Save a summary of all debugging steps
     */
    fun saveDebugSummary() {
        viewModelScope.launch {
            try {
                val summaryImage = preprocessingVisualizer.generateProcessingSummary()
                if (summaryImage != null) {
                    preprocessingVisualizer.saveVisualizationToGallery(
                        summaryImage,
                        "debug_summary"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving debug summary: ${e.message}", e)
            }
        }
    }
    
    /**
     * Get preprocessing configuration for specified receipt type
     */
    private fun getConfigForType(receiptType: ReceiptType): ReceiptPreprocessingConfig {
        return when (receiptType) {
            ReceiptType.AUTO_DETECT -> ReceiptPreprocessingConfig()
            ReceiptType.THERMAL -> ReceiptPreprocessingConfigFactory.thermalReceiptConfig()
            ReceiptType.INKJET, ReceiptType.LASER -> ReceiptPreprocessingConfigFactory.formalReceiptConfig()
            ReceiptType.DIGITAL -> ReceiptPreprocessingConfig(receiptType = ReceiptType.DIGITAL)
        }
    }
}