package com.example.struku.presentation.debug

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.struku.data.ocr.PreprocessingVisualizer
import com.example.struku.presentation.components.ProcessingStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PreprocessingDebugViewModel @Inject constructor(
    private val visualizer: PreprocessingVisualizer
) : ViewModel() {

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
        // Initialize visualizer in debug mode
        visualizer.setDebugMode(true)
        refreshProcessingSteps()
    }
    
    /**
     * Process an image with the visualizer
     */
    fun processImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _isProcessing.value = true
            
            // Reset visualizer and store original image
            visualizer.startNewSession()
            _currentImage.value = bitmap
            
            // TODO: Implement actual processing logic here
            
            _isProcessing.value = false
            refreshProcessingSteps()
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
            
            // TODO: Implement comparison processing logic
            
            _isProcessing.value = false
            refreshProcessingSteps()
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
        }
    }
    
    /**
     * Save a processing step image
     */
    fun saveStepImage(step: PreprocessingVisualizer.ProcessingStep) {
        viewModelScope.launch {
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
            val summaryImage = visualizer.generateProcessingSummary()
            if (summaryImage != null) {
                val fileName = "struku_summary_${System.currentTimeMillis()}"
                visualizer.saveVisualizationToGallery(summaryImage, fileName)
                // TODO: Show success message
            }
        }
    }
    
    /**
     * Refresh the processing steps from the visualizer
     */
    private fun refreshProcessingSteps() {
        _processingSteps.value = visualizer.processingSteps
    }
}