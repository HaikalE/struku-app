package com.example.struku.presentation.debug

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.struku.data.ocr.AdvancedImagePreprocessor
import com.example.struku.data.ocr.PreprocessingLevel
import com.example.struku.data.ocr.PreprocessingVisualizer
import com.example.struku.data.ocr.ReceiptPreprocessingConfig
import com.example.struku.data.ocr.ReceiptPreprocessingConfigFactory
import com.example.struku.data.ocr.ReceiptType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for debugging preprocessing pipeline
 */
@HiltViewModel
class PreprocessingDebugViewModel @Inject constructor(
    private val imagePreprocessor: AdvancedImagePreprocessor,
    private val preprocessingVisualizer: PreprocessingVisualizer
) : ViewModel() {
    
    private val TAG = "PreprocessingDebugVM"
    
    // Current status
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()
    
    // Debug mode
    private val _isDebugMode = MutableStateFlow(true)
    val isDebugMode = _isDebugMode.asStateFlow()
    
    // Preprocessing steps from visualizer
    val processingSteps = preprocessingVisualizer.processingSteps
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )
    
    // Last processed image
    private val _processedImage = MutableStateFlow<Bitmap?>(null)
    val processedImage = _processedImage.asStateFlow()
    
    init {
        // Enable debug mode by default
        preprocessingVisualizer.setDebugMode(true)
    }
    
    /**
     * Enable/disable debug mode
     */
    fun setDebugMode(enabled: Boolean) {
        _isDebugMode.value = enabled
        preprocessingVisualizer.setDebugMode(enabled)
    }
    
    /**
     * Reset debug session
     */
    fun resetSession() {
        preprocessingVisualizer.startNewSession()
    }
    
    /**
     * Process image with full preprocessing pipeline
     */
    fun processImage(bitmap: Bitmap, config: ReceiptPreprocessingConfig? = null) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                
                val effectiveConfig = config ?: ReceiptPreprocessingConfig(
                    receiptType = ReceiptType.AUTO_DETECT,
                    preprocessingLevel = PreprocessingLevel.ADVANCED,
                    enabled = true
                )
                
                // Process image with full pipeline
                val processedImage = withContext(Dispatchers.Default) {
                    imagePreprocessor.processReceiptImage(bitmap, effectiveConfig)
                }
                
                _processedImage.value = processedImage
                
                // Create before-after comparison
                if (_isDebugMode.value) {
                    val comparisonImage = preprocessingVisualizer.generateBeforeAfterComparison(
                        bitmap,
                        processedImage,
                        "Receipt Preprocessing"
                    )
                    
                    preprocessingVisualizer.captureProcessingStep(
                        "Final Comparison",
                        "Comparison of original vs fully processed image",
                        comparisonImage
                    )
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image: ${e.message}", e)
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * Save preprocessing step image
     */
    fun saveStepImage(step: PreprocessingVisualizer.ProcessingStep): File? {
        return preprocessingVisualizer.saveVisualizationToGallery(
            step.imageBitmap,
            "step_${step.id}_${step.name.replace(" ", "_")}"
        )
    }
    
    /**
     * Save debug summary
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
     * Process image with various configurations for comparison
     */
    fun processComparisons(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                
                // Reset session to start with clean slate
                resetSession()
                
                // Original image
                preprocessingVisualizer.captureProcessingStep(
                    "Original Input", 
                    "Raw input image",
                    bitmap
                )
                
                // Run different processing configs in parallel
                val configs = listOf(
                    ReceiptPreprocessingConfigFactory.createConfig(
                        receiptType = ReceiptType.AUTO_DETECT,
                        preprocessingLevel = PreprocessingLevel.BASIC
                    ),
                    ReceiptPreprocessingConfigFactory.createConfig(
                        receiptType = ReceiptType.AUTO_DETECT,
                        preprocessingLevel = PreprocessingLevel.MAXIMUM
                    ),
                    ReceiptPreprocessingConfigFactory.createConfig(
                        receiptType = ReceiptType.THERMAL,
                        preprocessingLevel = PreprocessingLevel.ADVANCED
                    ),
                    ReceiptPreprocessingConfigFactory.createConfig(
                        receiptType = ReceiptType.INKJET,
                        preprocessingLevel = PreprocessingLevel.ADVANCED
                    )
                )
                
                for (config in configs) {
                    processWithConfig(bitmap, config)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in comparison processing: ${e.message}", e)
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * Process image with specific configuration
     */
    private suspend fun processWithConfig(bitmap: Bitmap, config: ReceiptPreprocessingConfig) {
        try {
            val configName = when (config.receiptType) {
                ReceiptType.THERMAL -> "Thermal Receipt"
                ReceiptType.INKJET -> "Inkjet Receipt"
                ReceiptType.LASER -> "Laser Receipt"
                ReceiptType.DIGITAL -> "Digital Receipt"
                ReceiptType.AUTO_DETECT -> {
                    when (config.preprocessingLevel) {
                        PreprocessingLevel.BASIC -> "Speed Optimized"
                        PreprocessingLevel.MAXIMUM -> "Quality Optimized"
                        PreprocessingLevel.ADVANCED -> "Auto Detect"
                    }
                }
            }
            
            val processedImage = withContext(Dispatchers.Default) {
                imagePreprocessor.processReceiptImage(bitmap, config)
            }
            
            // Create comparison
            val comparisonImage = preprocessingVisualizer.generateBeforeAfterComparison(
                bitmap,
                processedImage,
                "Config: $configName"
            )
            
            preprocessingVisualizer.captureProcessingStep(
                "Config: $configName",
                "Processing with ${config.receiptType} configuration at ${config.preprocessingLevel} level",
                comparisonImage
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing with config ${config.receiptType}: ${e.message}", e)
        }
    }
}
