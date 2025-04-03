package com.example.struku.presentation.debug

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.struku.data.ocr.AdvancedImagePreprocessor
import com.example.struku.data.ocr.PreprocessingVisualizer
import com.example.struku.data.ocr.ReceiptPreprocessingConfig
import com.example.struku.data.ocr.ReceiptPreprocessingConfigFactory
import com.example.struku.data.ocr.ReceiptType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for OCR preprocessing debugging
 */
@HiltViewModel
class PreprocessingDebugViewModel @Inject constructor(
    private val imagePreprocessor: AdvancedImagePreprocessor,
    private val preprocessingVisualizer: PreprocessingVisualizer,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    companion object {
        private const val TAG = "PreprocessingDebugVM"
    }
    
    // Status saat ini
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()
    
    // Mode debug
    private val _isDebugMode = MutableStateFlow(true)
    val isDebugMode = _isDebugMode.asStateFlow()
    
    // Status message
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage = _statusMessage.asStateFlow()
    
    // Langkah preprocessing dari visualizer
    val processingSteps = preprocessingVisualizer.processingSteps
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )
    
    // Last processed image
    private val _processedImage = MutableStateFlow<Bitmap?>(null)
    val processedImage = _processedImage.asStateFlow()
    
    // Selected preprocessing config
    private val _selectedConfig = MutableStateFlow(ReceiptPreprocessingConfig())
    val selectedConfig = _selectedConfig.asStateFlow()
    
    // Available configs
    val availableConfigs = listOf(
        Pair("Auto Detect", ReceiptPreprocessingConfig()),
        Pair("Thermal Receipt", ReceiptPreprocessingConfigFactory.thermalReceiptConfig()),
        Pair("Formal Receipt", ReceiptPreprocessingConfigFactory.formalReceiptConfig()),
        Pair("Speed Optimized", ReceiptPreprocessingConfigFactory.speedOptimizedConfig()),
        Pair("Quality Optimized", ReceiptPreprocessingConfigFactory.qualityOptimizedConfig())
    )
    
    init {
        // Aktifkan mode debug secara default
        preprocessingVisualizer.setDebugMode(true)
    }
    
    /**
     * Aktifkan/nonaktifkan mode debug
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
     * Set selected config by index
     */
    fun setConfigByIndex(index: Int) {
        if (index in availableConfigs.indices) {
            _selectedConfig.value = availableConfigs[index].second
        }
    }
    
    /**
     * Proses gambar dari Uri
     */
    fun processImageFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _statusMessage.value = "Loading image..."
                
                // Load bitmap from uri
                val bitmap = withContext(Dispatchers.IO) {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                
                _statusMessage.value = "Processing image with ${selectedConfig.value.receiptType} configuration..."
                
                // Process the image
                processImage(bitmap)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image from URI: ${e.message}", e)
                _statusMessage.value = "Error: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * Proses gambar dengan pipeline preprocessing lengkap
     */
    fun processImage(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                
                // Get the current selected config
                val config = selectedConfig.value
                
                // Proses gambar dengan pipeline lengkap
                val processedImage = withContext(Dispatchers.Default) {
                    imagePreprocessor.processReceiptImage(bitmap, config)
                }
                
                _processedImage.value = processedImage
                _statusMessage.value = "Processing complete. ${processingSteps.value.size} steps recorded."
                
                // Create before-after comparison
                if (_isDebugMode.value) {
                    val comparisonImage = preprocessingVisualizer.generateBeforeAfterComparison(
                        bitmap,
                        processedImage,
                        "Receipt Preprocessing (${config.receiptType})"
                    )
                    
                    preprocessingVisualizer.captureProcessingStep(
                        "Final Comparison",
                        "Comparison of original vs fully processed image using ${config.receiptType} configuration",
                        comparisonImage
                    )
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image: ${e.message}", e)
                _statusMessage.value = "Error: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * Run comparative analysis with different configurations
     */
    fun runComparativeAnalysis(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _statusMessage.value = "Running comparative analysis..."
                
                val configs = listOf(
                    ReceiptType.THERMAL,
                    ReceiptType.INKJET,
                    ReceiptType.LASER,
                    ReceiptType.DIGITAL
                )
                
                withContext(Dispatchers.Default) {
                    // Process original with default settings
                    val defaultResult = imagePreprocessor.processReceiptImage(
                        bitmap, 
                        ReceiptPreprocessingConfig()
                    )
                    
                    // Create comparison with default
                    val defaultComparison = preprocessingVisualizer.generateBeforeAfterComparison(
                        bitmap,
                        defaultResult,
                        "Auto Detect Configuration"
                    )
                    
                    preprocessingVisualizer.captureProcessingStep(
                        "Default Comparison",
                        "Comparison with auto-detected receipt type",
                        defaultComparison
                    )
                    
                    // Process with specific configs
                    configs.forEach { receiptType ->
                        _statusMessage.value = "Processing with $receiptType configuration..."
                        
                        val config = ReceiptPreprocessingConfig(
                            receiptType = receiptType,
                            enabled = true
                        )
                        
                        val processedImage = imagePreprocessor.processReceiptImage(bitmap, config)
                        
                        val comparisonImage = preprocessingVisualizer.generateBeforeAfterComparison(
                            bitmap,
                            processedImage,
                            "Receipt Type: $receiptType"
                        )
                        
                        preprocessingVisualizer.captureProcessingStep(
                            "Comparison: $receiptType",
                            "Optimized for receipt type: $receiptType",
                            comparisonImage
                        )
                    }
                    
                    // Compare speed vs quality
                    _statusMessage.value = "Comparing speed vs quality configurations..."
                    
                    val speedResult = imagePreprocessor.processReceiptImage(
                        bitmap,
                        ReceiptPreprocessingConfigFactory.speedOptimizedConfig()
                    )
                    
                    val qualityResult = imagePreprocessor.processReceiptImage(
                        bitmap,
                        ReceiptPreprocessingConfigFactory.qualityOptimizedConfig()
                    )
                    
                    val speedQualityComparison = preprocessingVisualizer.generateBeforeAfterComparison(
                        speedResult,
                        qualityResult,
                        "Speed vs Quality Optimization"
                    )
                    
                    preprocessingVisualizer.captureProcessingStep(
                        "Speed vs Quality",
                        "Left: Speed optimized, Right: Quality optimized",
                        speedQualityComparison
                    )
                }
                
                _statusMessage.value = "Comparative analysis complete. ${processingSteps.value.size} steps recorded."
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in comparative analysis: ${e.message}", e)
                _statusMessage.value = "Error: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * Simpan gambar tahapan preprocessing
     */
    fun saveStepImage(step: PreprocessingVisualizer.ProcessingStep): File? {
        val file = preprocessingVisualizer.saveVisualizationToGallery(
            step.imageBitmap,
            "step_${step.id}_${step.name.replace(" ", "_")}"
        )
        
        if (file != null) {
            _statusMessage.value = "Saved to ${file.absolutePath}"
        } else {
            _statusMessage.value = "Failed to save image"
        }
        
        return file
    }
    
    /**
     * Simpan ringkasan debug
     */
    fun saveDebugSummary() {
        viewModelScope.launch {
            try {
                val summaryImage = preprocessingVisualizer.generateProcessingSummary()
                if (summaryImage != null) {
                    val file = preprocessingVisualizer.saveVisualizationToGallery(
                        summaryImage,
                        "debug_summary"
                    )
                    
                    if (file != null) {
                        _statusMessage.value = "Summary saved to ${file.absolutePath}"
                    } else {
                        _statusMessage.value = "Failed to save summary"
                    }
                } else {
                    _statusMessage.value = "No summary to save. Process an image first."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving debug summary: ${e.message}", e)
                _statusMessage.value = "Error saving summary: ${e.message}"
            }
        }
    }
    
    /**
     * Export all debug data as a ZIP file
     */
    fun exportDebugData(): File? {
        try {
            // Create a folder in the app's private directory
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filename = "struku_debug_$timestamp.zip"
            
            val outputFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                filename
            )
            
            // TODO: Implement ZIP file creation from debug folder
            
            _statusMessage.value = "Debug data exported to ${outputFile.absolutePath}"
            return outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting debug data: ${e.message}", e)
            _statusMessage.value = "Error exporting debug data: ${e.message}"
            return null
        }
    }
    
    /**
     * Clear status message
     */
    fun clearStatusMessage() {
        _statusMessage.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        
        // Clean up resources if necessary
    }
}