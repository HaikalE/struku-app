package com.example.struku.presentation.debug

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.struku.data.ocr.AdvancedImagePreprocessor
import com.example.struku.data.ocr.PreprocessingVisualizer
import com.example.struku.data.ocr.ReceiptPreprocessingConfig
import com.example.struku.data.ocr.ReceiptPreprocessingConfigFactory
import com.example.struku.presentation.components.ProcessingStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PreprocessingDebugViewModel @Inject constructor(
    private val visualizer: PreprocessingVisualizer,
    private val imagePreprocessor: AdvancedImagePreprocessor
) : ViewModel() {

    private val TAG = "PreprocessingDebugVM"

    // State flows for UI state
    private val _processingSteps = MutableStateFlow<List<PreprocessingVisualizer.ProcessingStep>>(emptyList())
    val processingSteps: StateFlow<List<PreprocessingVisualizer.ProcessingStep>> = _processingSteps.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    private val _isDebugMode = MutableStateFlow(true)
    val isDebugMode: StateFlow<Boolean> = _isDebugMode.asStateFlow()
    
    private val _currentImage = MutableStateFlow<Bitmap?>(null)
    val currentImage: StateFlow<Bitmap?> = _currentImage.asStateFlow()
    
    private val _currentProcessingStep = MutableStateFlow(ProcessingStep.ORIGINAL)
    val currentProcessingStep: StateFlow<ProcessingStep> = _currentProcessingStep.asStateFlow()
    
    init {
        // Initialize visualizer in debug mode with proper settings
        visualizer.setDebugMode(true)
        visualizer.setSamplingRate(1) // Capture all steps in debug mode
        Log.d(TAG, "ViewModel initialized with debug mode ON")
        refreshProcessingSteps()
    }
    
    /**
     * Process an image with the visualizer
     */
    fun processImage(bitmap: Bitmap) {
        viewModelScope.launch {
            Log.d(TAG, "Processing image, size: ${bitmap.width}x${bitmap.height}")
            _isProcessing.value = true
            
            // Reset visualizer and store original image
            visualizer.startNewSession()
            _currentImage.value = bitmap
            
            // Process the image with various configurations
            try {
                // Create a default config
                val config = ReceiptPreprocessingConfigFactory.createConfig()
                
                // Ensure debug mode is enabled
                visualizer.setDebugMode(true)
                visualizer.setSamplingRate(1)
                
                // Process the image
                val processedImage = imagePreprocessor.processReceiptImage(bitmap, config)
                
                // Update the current image to the processed one
                _currentImage.value = processedImage
                Log.d(TAG, "Image processing complete. Steps captured: ${visualizer.processingSteps.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image: ${e.message}", e)
            } finally {
                _isProcessing.value = false
                refreshProcessingSteps()
            }
        }
    }
    
    /**
     * Process image with different comparison settings
     */
    fun processComparisons(bitmap: Bitmap) {
        viewModelScope.launch {
            _isProcessing.value = true
            
            // Reset visualizer
            visualizer.startNewSession()
            _currentImage.value = bitmap
            
            // Process with different settings
            try {
                // Create configurations for different preprocessing levels
                val basicConfig = ReceiptPreprocessingConfigFactory.createBasicConfig()
                val advancedConfig = ReceiptPreprocessingConfigFactory.createAdvancedConfig()
                val maxConfig = ReceiptPreprocessingConfigFactory.createMaxConfig()
                
                // Ensure debug mode is enabled
                visualizer.setDebugMode(true)
                visualizer.setSamplingRate(1)
                
                // Process with all configurations
                imagePreprocessor.processReceiptImage(bitmap, basicConfig)
                imagePreprocessor.processReceiptImage(bitmap, advancedConfig)
                val processedImage = imagePreprocessor.processReceiptImage(bitmap, maxConfig)
                
                // Update the current image to the processed one
                _currentImage.value = processedImage
                Log.d(TAG, "Comparison processing complete. Steps captured: ${visualizer.processingSteps.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Error in comparison processing: ${e.message}", e)
            } finally {
                _isProcessing.value = false
                refreshProcessingSteps()
            }
        }
    }
    
    /**
     * Set current processing step
     */
    fun setCurrentProcessingStep(step: ProcessingStep) {
        _currentProcessingStep.value = step
    }
    
    /**
     * Toggle debug mode
     */
    fun setDebugMode(enabled: Boolean) {
        _isDebugMode.value = enabled
        visualizer.setDebugMode(enabled)
        
        // Always use sampling rate 1 in debug mode
        if (enabled) {
            visualizer.setSamplingRate(1)
        }
        
        Log.d(TAG, "Debug mode set to: $enabled")
    }
    
    /**
     * Reset the debug session
     */
    fun resetSession() {
        viewModelScope.launch {
            visualizer.startNewSession()
            _currentImage.value = null
            _currentProcessingStep.value = ProcessingStep.ORIGINAL
            refreshProcessingSteps()
            Log.d(TAG, "Debug session reset")
        }
    }
    
    /**
     * Save a processing step image
     */
    fun saveStepImage(step: PreprocessingVisualizer.ProcessingStep) {
        viewModelScope.launch {
            Log.d(TAG, "Saving step image: ${step.name}")
            val fileName = "struku_step_${step.id}_${step.name.replace(" ", "_").lowercase()}"
            visualizer.saveVisualizationToGallery(step.imageBitmap, fileName)
            // TODO: Show success message
        }
    }
    
    /**
     * Save a summary of all processing steps
     */
    fun saveDebugSummary() {
        viewModelScope.launch {
            Log.d(TAG, "Generating and saving summary")
            val summaryImage = visualizer.generateProcessingSummary()
            if (summaryImage != null) {
                val fileName = "struku_summary_${System.currentTimeMillis()}"
                visualizer.saveVisualizationToGallery(summaryImage, fileName)
                // TODO: Show success message
            } else {
                Log.w(TAG, "Failed to generate summary - no images")
            }
        }
    }
    
    /**
     * Refresh the processing steps from the visualizer
     */
    private fun refreshProcessingSteps() {
        val steps = visualizer.processingSteps
        _processingSteps.value = steps
        Log.d(TAG, "Refreshed processing steps: ${steps.size} steps")
    }
}
