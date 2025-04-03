package com.example.struku.data.ocr

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tool for visualizing image preprocessing steps for debugging
 * Optimized to reduce performance overhead
 */
@Singleton
class PreprocessingVisualizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val tag = "ProcessingVisualizer"
    
    // Debug mode flag - default to false to reduce overhead
    private var debugMode = false
    
    // Sampling rate to reduce overhead (only capture 1 in N processing steps)
    private var samplingRate = 2
    private var stepCounter = 0
    
    // Keep track of processing steps for visualization
    private val _processingSteps = mutableStateListOf<ProcessingStep>()
    val processingSteps: List<ProcessingStep> = _processingSteps
    
    // Max number of steps to store to avoid memory issues
    private val maxStoredSteps = 10
    
    /**
     * A single step in the preprocessing pipeline
     */
    data class ProcessingStep(
        val id: String = System.currentTimeMillis().toString(),
        val name: String,
        val description: String,
        val imageBitmap: Bitmap,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Enable or disable debug mode
     */
    fun setDebugMode(enabled: Boolean) {
        debugMode = enabled
    }
    
    /**
     * Start a new debug session
     */
    fun startNewSession() {
        _processingSteps.clear()
        stepCounter = 0
    }
    
    /**
     * Clear all processing steps
     */
    fun clear() {
        _processingSteps.clear()
        stepCounter = 0
    }
    
    /**
     * Set sampling rate to control visualization overhead
     */
    fun setSamplingRate(rate: Int) {
        samplingRate = rate.coerceAtLeast(1)
    }
    
    /**
     * Capture a processing step for visualization with reduced overhead
     */
    fun captureProcessingStep(name: String, description: String, image: Bitmap) {
        if (!debugMode) return
        
        // Increment counter
        stepCounter++
        
        // Only capture every Nth step to reduce overhead
        if (stepCounter % samplingRate != 0) return
        
        Log.d(tag, "Captured processing step: $name")
        
        // Create a downsampled version of the image to save memory
        val downsampledImage = downsampleBitmap(image)
        
        // Add to processing steps, ensuring we don't exceed memory constraints
        synchronized(_processingSteps) {
            // Remove oldest step if we've reached max capacity
            if (_processingSteps.size >= maxStoredSteps) {
                _processingSteps.removeAt(0)
            }
            
            _processingSteps.add(
                ProcessingStep(
                    name = name,
                    description = description,
                    imageBitmap = downsampledImage
                )
            )
        }
    }
    
    /**
     * Downsample a bitmap to reduce memory usage
     */
    private fun downsampleBitmap(original: Bitmap): Bitmap {
        val maxDimension = 400 // Max dimension for stored preview
        
        // If image is already small enough, use it directly
        if (original.width <= maxDimension && original.height <= maxDimension) {
            return original
        }
        
        // Calculate new dimensions
        val ratio = original.width.toFloat() / original.height.toFloat()
        val newWidth: Int
        val newHeight: Int
        
        if (original.width > original.height) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt()
        }
        
        // Create downsampled bitmap
        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true)
    }
    
    /**
     * Generate a comparison image showing before and after
     */
    fun generateBeforeAfterComparison(
        before: Bitmap,
        after: Bitmap,
        title: String
    ): Bitmap {
        // Create a new bitmap with both images side by side
        val width = before.width + after.width
        val height = Math.max(before.height, after.height) + 80 // Extra space for title
        
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        
        // Fill background
        canvas.drawColor(Color.WHITE)
        
        // Draw title
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 40f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(title, width / 2f, 50f, paint)
        
        // Draw labels
        paint.textSize = 30f
        canvas.drawText("Original", before.width / 2f, 80f, paint)
        canvas.drawText("Processed", before.width + after.width / 2f, 80f, paint)
        
        // Draw images
        canvas.drawBitmap(before, 0f, 100f, null)
        canvas.drawBitmap(after, before.width.toFloat(), 100f, null)
        
        return result
    }
    
    /**
     * Generate a summary image of all processing steps
     */
    fun generateProcessingSummary(): Bitmap? {
        synchronized(_processingSteps) {
            if (_processingSteps.isEmpty()) return null
            
            // Create a grid of images
            val numImages = _processingSteps.size
            val cols = Math.min(2, numImages) // Reduced column count for better overview
            val rows = (numImages + cols - 1) / cols
            
            // Sample image to get dimensions
            val sampleBitmap = _processingSteps[0].imageBitmap
            val thumbWidth = 320 // Reduced thumbnail size
            val thumbHeight = (thumbWidth * sampleBitmap.height.toFloat() / sampleBitmap.width).toInt()
            
            val result = Bitmap.createBitmap(
                thumbWidth * cols, 
                thumbHeight * rows + 80, 
                Bitmap.Config.ARGB_8888
            )
            
            val canvas = Canvas(result)
            canvas.drawColor(Color.WHITE)
            
            // Draw title
            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 40f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Processing Steps", result.width / 2f, 50f, paint)
            
            // Draw all images in grid
            paint.textSize = 20f
            
            for (i in 0 until numImages) {
                val step = _processingSteps[i]
                val col = i % cols
                val row = i / cols
                
                val x = col * thumbWidth
                val y = row * thumbHeight + 80
                
                // Images are already downsampled in captureProcessingStep
                val thumbnail = step.imageBitmap
                
                canvas.drawBitmap(thumbnail, x.toFloat(), y.toFloat(), null)
                
                // Draw step name
                canvas.drawText(
                    step.name, 
                    x + thumbWidth / 2f, 
                    y + 30f, 
                    paint
                )
            }
            
            return result
        }
    }
}
