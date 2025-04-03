package com.example.struku.data.ocr

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tool for visualizing image preprocessing steps for debugging
 */
@Singleton
class PreprocessingVisualizer @Inject constructor() {
    
    private val tag = "ProcessingVisualizer"
    
    // Keep track of processing steps for visualization
    private val _processedImages = mutableStateListOf<ProcessingStep>()
    val processedImages: List<ProcessingStep> = _processedImages
    
    /**
     * A single step in the preprocessing pipeline
     */
    data class ProcessingStep(
        val name: String,
        val description: String,
        val image: Bitmap,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Clear all processing steps
     */
    fun clear() {
        _processedImages.clear()
    }
    
    /**
     * Capture a processing step for visualization
     */
    fun captureProcessingStep(name: String, description: String, image: Bitmap) {
        Log.d(tag, "Captured processing step: $name")
        _processedImages.add(ProcessingStep(name, description, image))
    }
}