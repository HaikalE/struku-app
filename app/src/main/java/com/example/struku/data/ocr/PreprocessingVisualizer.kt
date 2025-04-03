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
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tool for visualizing image preprocessing steps for debugging
 */
@Singleton
class PreprocessingVisualizer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val tag = "ProcessingVisualizer"
    
    // Debug mode flag
    private var debugMode = false
    
    // Keep track of processing steps for visualization
    private val _processingSteps = mutableStateListOf<ProcessingStep>()
    val processingSteps: List<ProcessingStep> = _processingSteps
    
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
    }
    
    /**
     * Clear all processing steps
     */
    fun clear() {
        _processingSteps.clear()
    }
    
    /**
     * Capture a processing step for visualization
     */
    fun captureProcessingStep(name: String, description: String, image: Bitmap) {
        if (!debugMode) return
        
        Log.d(tag, "Captured processing step: $name")
        _processingSteps.add(ProcessingStep(
            name = name,
            description = description,
            imageBitmap = image
        ))
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
        if (_processingSteps.isEmpty()) return null
        
        // Create a grid of images
        val numImages = _processingSteps.size
        val cols = Math.min(3, numImages)
        val rows = (numImages + cols - 1) / cols
        
        // Sample image to get dimensions
        val sampleBitmap = _processingSteps[0].imageBitmap
        val thumbWidth = 400
        val thumbHeight = (thumbWidth * sampleBitmap.height.toFloat() / sampleBitmap.width).toInt()
        
        val result = Bitmap.createBitmap(
            thumbWidth * cols, 
            thumbHeight * rows + 100, 
            Bitmap.Config.ARGB_8888
        )
        
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)
        
        // Draw title
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 50f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Processing Steps Summary", result.width / 2f, 60f, paint)
        
        // Draw all images in grid
        paint.textSize = 24f
        
        for (i in 0 until numImages) {
            val step = _processingSteps[i]
            val col = i % cols
            val row = i / cols
            
            val x = col * thumbWidth
            val y = row * thumbHeight + 100
            
            // Resize image to thumbnail
            val thumbnail = Bitmap.createScaledBitmap(
                step.imageBitmap, 
                thumbWidth, 
                thumbHeight, 
                false
            )
            
            canvas.drawBitmap(thumbnail, x.toFloat(), y.toFloat(), null)
            
            // Draw step name
            canvas.drawText(
                step.name, 
                x + thumbWidth / 2f, 
                y + 40f, 
                paint
            )
        }
        
        return result
    }
    
    /**
     * Save a visualization to the device gallery
     */
    fun saveVisualizationToGallery(bitmap: Bitmap, fileName: String): File? {
        try {
            val storageDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "StruKu"
            ).apply {
                if (!exists()) {
                    mkdirs()
                }
            }
            
            val imageFile = File(storageDir, "${fileName}.png")
            
            val outputStream: OutputStream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "${fileName}.png")
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/StruKu")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                resolver.openOutputStream(uri!!)!!
            } else {
                FileOutputStream(imageFile)
            }
            
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            
            return imageFile
        } catch (e: Exception) {
            Log.e(tag, "Error saving image: ${e.message}", e)
            return null
        }
    }
}
