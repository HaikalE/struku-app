package com.example.struku.presentation.debug

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.struku.data.ocr.AdvancedImagePreprocessor
import com.example.struku.data.ocr.PreprocessingVisualizer
import com.example.struku.data.ocr.ReceiptPreprocessingConfig
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
 * ViewModel untuk debugging pipeline preprocessing OCR
 */
@HiltViewModel
class PreprocessingDebugViewModel @Inject constructor(
    private val imagePreprocessor: AdvancedImagePreprocessor,
    private val preprocessingVisualizer: PreprocessingVisualizer
) : ViewModel() {
    
    private val TAG = "PreprocessingDebugVM"
    
    // Status saat ini
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()
    
    // Mode debug
    private val _isDebugMode = MutableStateFlow(true)
    val isDebugMode = _isDebugMode.asStateFlow()
    
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
     * Proses gambar dengan pipeline preprocessing lengkap
     */
    fun processImage(bitmap: Bitmap, config: ReceiptPreprocessingConfig? = null) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                
                val effectiveConfig = config ?: ReceiptPreprocessingConfig(
                    receiptType = ReceiptType.AUTO_DETECT,
                    enabled = true
                )
                
                // Proses gambar dengan pipeline lengkap
                val processedImage = withContext(Dispatchers.Default) {
                    imagePreprocessor.processReceiptImage(bitmap, effectiveConfig)
                }
                
                _processedImage.value = processedImage
                
                // Buat perbandingan sebelum-sesudah
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
     * Simpan gambar tahapan preprocessing
     */
    fun saveStepImage(step: PreprocessingVisualizer.ProcessingStep): File? {
        return preprocessingVisualizer.saveVisualizationToGallery(
            step.imageBitmap,
            "step_${step.id}_${step.name.replace(" ", "_")}"
        )
    }
    
    /**
     * Simpan ringkasan debug
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
     * Proses gambar dengan konfigurasi yang berbeda untuk perbandingan
     */
    fun processForComparison(bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                
                // Buat beberapa konfigurasi untuk perbandingan
                val configTypes = listOf(
                    ReceiptType.THERMAL,
                    ReceiptType.INKJET,
                    ReceiptType.LASER,
                    ReceiptType.DIGITAL
                )
                
                val results = withContext(Dispatchers.Default) {
                    configTypes.map { receiptType ->
                        val config = ReceiptPreprocessingConfig(
                            receiptType = receiptType,
                            enabled = true
                        )
                        
                        val processedImage = imagePreprocessor.processReceiptImage(bitmap, config)
                        
                        Pair(receiptType, processedImage)
                    }
                }
                
                // Catat hasil perbandingan
                if (_isDebugMode.value) {
                    results.forEach { (receiptType, processedImage) ->
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
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in comparison processing: ${e.message}", e)
            } finally {
                _isProcessing.value = false
            }
        }
    }
}