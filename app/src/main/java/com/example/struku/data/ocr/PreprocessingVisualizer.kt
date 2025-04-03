package com.example.struku.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Visualisasi hasil dari setiap tahap pre-processing untuk keperluan debugging dan fine-tuning
 */
@Singleton
class PreprocessingVisualizer @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "PreprocessingVisualizer"
        private const val DEBUG_DIR = "struku_debug"
        private const val MAX_STORED_SESSIONS = 5
    }
    
    // Menyimpan gambar-gambar hasil preprocessing dari sesi terakhir
    private val _processingSteps = MutableStateFlow<List<ProcessingStep>>(emptyList())
    val processingSteps = _processingSteps.asStateFlow()
    
    private var currentSessionId: String = generateSessionId()
    private var debugMode: Boolean = false
    
    // Model untuk menyimpan tahapan preprocessing
    data class ProcessingStep(
        val id: String,
        val name: String,
        val description: String,
        val imageBitmap: Bitmap,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Aktifkan atau nonaktifkan mode debug
     */
    fun setDebugMode(enabled: Boolean) {
        debugMode = enabled
        if (!enabled) {
            // Clear steps when disabling debug mode
            _processingSteps.value = emptyList()
        } else {
            // Create a new session when enabling debug mode
            currentSessionId = generateSessionId()
            createDebugDirectory()
        }
    }
    
    /**
     * Reset sesi debugging dan mulai sesi baru
     */
    fun startNewSession() {
        currentSessionId = generateSessionId()
        _processingSteps.value = emptyList()
        cleanupOldSessions()
    }
    
    /**
     * Catat gambar hasil tahapan preprocessing
     */
    fun captureProcessingStep(
        stepName: String,
        description: String, 
        bitmap: Bitmap
    ) {
        if (!debugMode) return
        
        try {
            val stepId = "${_processingSteps.value.size + 1}"
            val newStep = ProcessingStep(
                id = stepId,
                name = stepName,
                description = description,
                imageBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
            )
            
            // Add to in-memory collection
            val updatedSteps = _processingSteps.value.toMutableList()
            updatedSteps.add(newStep)
            _processingSteps.value = updatedSteps
            
            // Save to storage if debug mode is on
            if (debugMode) {
                saveStepToStorage(newStep)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing processing step: ${e.message}", e)
        }
    }
    
    /**
     * Capture Mat dari OpenCV
     */
    fun captureProcessingStepMat(
        stepName: String, 
        description: String, 
        mat: Mat
    ) {
        if (!debugMode) return
        
        try {
            val bitmap = Bitmap.createBitmap(
                mat.cols(), 
                mat.rows(), 
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(mat, bitmap)
            captureProcessingStep(stepName, description, bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing OpenCV Mat: ${e.message}", e)
        }
    }
    
    /**
     * Generate visualisasi perbandingan sebelum-sesudah
     */
    fun generateBeforeAfterComparison(
        originalBitmap: Bitmap,
        processedBitmap: Bitmap,
        title: String
    ): Bitmap {
        // Normalize sizes
        val maxWidth = maxOf(originalBitmap.width, processedBitmap.width)
        val maxHeight = maxOf(originalBitmap.height, processedBitmap.height)
        
        // Create a bitmap to hold both images side by side with labels
        val padding = 40
        val headerHeight = 80
        val resultBitmap = Bitmap.createBitmap(
            maxWidth * 2 + padding * 3, 
            maxHeight + headerHeight + padding * 2,
            Bitmap.Config.ARGB_8888
        )
        
        val canvas = Canvas(resultBitmap)
        canvas.drawColor(Color.BLACK)
        
        // Draw title
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 36f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            title,
            resultBitmap.width / 2f,
            padding + 40f,
            titlePaint
        )
        
        // Draw labels
        val labelPaint = Paint().apply {
            color = Color.WHITE
            textSize = 30f
        }
        canvas.drawText(
            "Original",
            padding + (maxWidth / 2f),
            headerHeight + 30f,
            labelPaint
        )
        canvas.drawText(
            "Processed",
            padding * 2 + maxWidth + (maxWidth / 2f),
            headerHeight + 30f,
            labelPaint
        )
        
        // Scale original bitmap to fit
        val originalScaled = scaleBitmapToFit(originalBitmap, maxWidth, maxHeight)
        canvas.drawBitmap(
            originalScaled,
            padding.toFloat(),
            (headerHeight + padding).toFloat(),
            null
        )
        
        // Scale processed bitmap to fit
        val processedScaled = scaleBitmapToFit(processedBitmap, maxWidth, maxHeight)
        canvas.drawBitmap(
            processedScaled,
            (padding * 2 + maxWidth).toFloat(),
            (headerHeight + padding).toFloat(),
            null
        )
        
        return resultBitmap
    }
    
    /**
     * Generate composite visualization dari semua steps
     */
    fun generateProcessingSummary(): Bitmap? {
        val steps = _processingSteps.value
        if (steps.isEmpty()) return null
        
        val stepCount = steps.size
        val cols = if (stepCount <= 3) stepCount else 3
        val rows = (stepCount + cols - 1) / cols
        
        val thumbSize = 300
        val padding = 20
        val headerHeight = 50
        
        val resultWidth = cols * (thumbSize + padding) + padding
        val resultHeight = rows * (thumbSize + headerHeight + padding) + padding
        
        val resultBitmap = Bitmap.createBitmap(
            resultWidth,
            resultHeight,
            Bitmap.Config.ARGB_8888
        )
        
        val canvas = Canvas(resultBitmap)
        canvas.drawColor(Color.BLACK)
        
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 14f
            textAlign = Paint.Align.CENTER
        }
        
        for (i in steps.indices) {
            val step = steps[i]
            
            val col = i % cols
            val row = i / cols
            
            val x = padding + col * (thumbSize + padding)
            val y = padding + row * (thumbSize + headerHeight + padding)
            
            // Draw thumbnail
            val thumbnail = scaleBitmapToFit(step.imageBitmap, thumbSize, thumbSize)
            canvas.drawBitmap(thumbnail, x.toFloat(), (y + headerHeight).toFloat(), null)
            
            // Draw step name
            canvas.drawText(
                "${i + 1}. ${step.name}",
                x + thumbSize/2f,
                y + 25f,
                titlePaint
            )
            
            // Draw separator line
            val linePaint = Paint().apply {
                color = Color.GRAY
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawLine(
                x.toFloat(),
                (y + headerHeight - 5).toFloat(),
                (x + thumbSize).toFloat(),
                (y + headerHeight - 5).toFloat(),
                linePaint
            )
        }
        
        return resultBitmap
    }
    
    /**
     * Simpan hasil visualisasi ke storage
     */
    fun saveVisualizationToGallery(bitmap: Bitmap, name: String): File? {
        try {
            val debugDir = getDebugDirectory() ?: return null
            
            val file = File(debugDir, "${name}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            
            return file
        } catch (e: Exception) {
            Log.e(TAG, "Error saving visualization: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Scale bitmap to fit within target dimensions while preserving aspect ratio
     */
    private fun scaleBitmapToFit(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        val scaleWidth = targetWidth.toFloat() / width
        val scaleHeight = targetHeight.toFloat() / height
        val scale = minOf(scaleWidth, scaleHeight)
        
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            width,
            height,
            matrix,
            true
        )
    }
    
    /**
     * Simpan step ke storage
     */
    private fun saveStepToStorage(step: ProcessingStep) {
        try {
            val debugDir = getDebugDirectory() ?: return
            val sessionDir = File(debugDir, currentSessionId)
            if (!sessionDir.exists()) {
                sessionDir.mkdirs()
            }
            
            val stepNumberPadded = step.id.padStart(2, '0')
            val file = File(sessionDir, "${stepNumberPadded}_${step.name.replace(" ", "_")}.jpg")
            
            FileOutputStream(file).use { out ->
                step.imageBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            
            // Create step info file
            val infoFile = File(sessionDir, "${stepNumberPadded}_${step.name.replace(" ", "_")}.txt")
            infoFile.writeText(
                "Name: ${step.name}\n" +
                "Description: ${step.description}\n" +
                "Timestamp: ${formatTimestamp(step.timestamp)}\n" +
                "Dimensions: ${step.imageBitmap.width}x${step.imageBitmap.height}"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving step to storage: ${e.message}", e)
        }
    }
    
    /**
     * Generate unique session ID
     */
    private fun generateSessionId(): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "session_$timeStamp"
    }
    
    /**
     * Format timestamp for display
     */
    private fun formatTimestamp(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
    
    /**
     * Get or create debug directory
     */
    private fun getDebugDirectory(): File? {
        val extDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: return null
        
        val debugDir = File(extDir, DEBUG_DIR)
        if (!debugDir.exists()) {
            debugDir.mkdirs()
        }
        
        return debugDir
    }
    
    /**
     * Create debug directory structure
     */
    private fun createDebugDirectory() {
        try {
            val debugDir = getDebugDirectory() ?: return
            val sessionDir = File(debugDir, currentSessionId)
            if (!sessionDir.exists()) {
                sessionDir.mkdirs()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating debug directory: ${e.message}", e)
        }
    }
    
    /**
     * Clean up old debug sessions
     */
    private fun cleanupOldSessions() {
        try {
            val debugDir = getDebugDirectory() ?: return
            
            val sessions = debugDir.listFiles { file -> 
                file.isDirectory && file.name.startsWith("session_") 
            }
            
            if (sessions == null || sessions.size <= MAX_STORED_SESSIONS) {
                return
            }
            
            // Sort by creation time, oldest first
            sessions.sortBy { it.lastModified() }
            
            // Delete oldest sessions
            val sessionsToDelete = sessions.size - MAX_STORED_SESSIONS
            for (i in 0 until sessionsToDelete) {
                sessions[i].deleteRecursively()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old sessions: ${e.message}", e)
        }
    }
}