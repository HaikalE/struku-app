package com.example.struku.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Advanced image preprocessor for improving OCR results
 */
@Singleton
class AdvancedImagePreprocessor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val visualizer: PreprocessingVisualizer
) {
    private val TAG = "AdvancedImagePreprocessor"
    
    /**
     * Process a receipt image with the specified configuration
     */
    fun processReceiptImage(bitmap: Bitmap, config: ReceiptPreprocessingConfig): Bitmap {
        if (!config.enabled) {
            return bitmap
        }
        
        // Start with a copy of the original
        var processedImage = bitmap.copy(bitmap.config, true)
        
        // Capture the original for debug
        visualizer.captureProcessingStep(
            "Original Image",
            "Input image before preprocessing",
            processedImage
        )
        
        try {
            // Apply processing based on level
            when (config.preprocessingLevel) {
                PreprocessingLevel.BASIC -> {
                    processedImage = applyBasicPreprocessing(processedImage, config)
                }
                PreprocessingLevel.ADVANCED -> {
                    processedImage = applyAdvancedPreprocessing(processedImage, config)
                }
                PreprocessingLevel.MAXIMUM -> {
                    processedImage = applyMaximumPreprocessing(processedImage, config)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during image preprocessing: ${e.message}", e)
            // Return original if processing fails
            return bitmap
        }
        
        return processedImage
    }
    
    /**
     * Apply basic preprocessing (grayscale, contrast)
     */
    private fun applyBasicPreprocessing(bitmap: Bitmap, config: ReceiptPreprocessingConfig): Bitmap {
        // Convert to grayscale
        val grayscaleBitmap = toGrayscale(bitmap)
        visualizer.captureProcessingStep(
            "Grayscale",
            "Converted to grayscale",
            grayscaleBitmap
        )
        
        // Increase contrast
        val contrastBitmap = adjustContrast(grayscaleBitmap, 1.5f)
        visualizer.captureProcessingStep(
            "Contrast Enhanced",
            "Increased contrast for better text visibility",
            contrastBitmap
        )
        
        return contrastBitmap
    }
    
    /**
     * Apply advanced preprocessing (grayscale, contrast, noise reduction)
     */
    private fun applyAdvancedPreprocessing(bitmap: Bitmap, config: ReceiptPreprocessingConfig): Bitmap {
        // First apply basic preprocessing
        var processedImage = applyBasicPreprocessing(bitmap, config)
        
        // Apply noise reduction
        processedImage = applyNoiseReduction(processedImage)
        visualizer.captureProcessingStep(
            "Noise Reduction",
            "Applied noise reduction filter",
            processedImage
        )
        
        // Apply adaptive thresholding based on receipt type
        when (config.receiptType) {
            ReceiptType.THERMAL -> {
                processedImage = applyAdaptiveThreshold(processedImage, 15, 2.0)
            }
            ReceiptType.INKJET, ReceiptType.LASER -> {
                processedImage = applyAdaptiveThreshold(processedImage, 11, 1.0)
            }
            else -> {
                processedImage = applyAdaptiveThreshold(processedImage, 11, 1.5)
            }
        }
        
        visualizer.captureProcessingStep(
            "Adaptive Threshold",
            "Applied threshold to enhance text vs background",
            processedImage
        )
        
        return processedImage
    }
    
    /**
     * Apply maximum preprocessing (everything + perspective correction, deskewing)
     */
    private fun applyMaximumPreprocessing(bitmap: Bitmap, config: ReceiptPreprocessingConfig): Bitmap {
        // First apply advanced preprocessing
        var processedImage = applyAdvancedPreprocessing(bitmap, config)
        
        // Apply additional processing
        
        // Sharpen the image
        processedImage = sharpenImage(processedImage)
        visualizer.captureProcessingStep(
            "Sharpening",
            "Applied sharpening filter",
            processedImage
        )
        
        // For thermal receipts, apply additional contrast
        if (config.receiptType == ReceiptType.THERMAL) {
            processedImage = adjustContrast(processedImage, 1.8f)
            visualizer.captureProcessingStep(
                "Extra Contrast",
                "Applied additional contrast for thermal paper",
                processedImage
            )
        }
        
        return processedImage
    }
    
    /**
     * Convert to grayscale
     */
    private fun toGrayscale(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val canvas = Canvas(result)
        val paint = Paint()
        
        // Apply grayscale color filter
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f) // 0 = grayscale
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }
    
    /**
     * Adjust contrast
     */
    private fun adjustContrast(bitmap: Bitmap, contrast: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val canvas = Canvas(result)
        val paint = Paint()
        
        // Apply contrast matrix
        val matrix = ColorMatrix()
        
        // Adjust the contrast
        val scale = contrast
        val translate = (-.5f * scale + .5f) * 255f
        
        matrix.set(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return result
    }
    
    /**
     * Apply noise reduction
     */
    private fun applyNoiseReduction(bitmap: Bitmap): Bitmap {
        // Simple implementation of a blur filter
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // Get pixels
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val resultPixels = IntArray(width * height)
        
        // Apply median filter
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                
                // Skip edge pixels
                if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
                    resultPixels[index] = pixels[index]
                    continue
                }
                
                // Get 3x3 neighborhood
                val neighbors = arrayOf(
                    pixels[(y - 1) * width + (x - 1)],
                    pixels[(y - 1) * width + x],
                    pixels[(y - 1) * width + (x + 1)],
                    pixels[y * width + (x - 1)],
                    pixels[index],
                    pixels[y * width + (x + 1)],
                    pixels[(y + 1) * width + (x - 1)],
                    pixels[(y + 1) * width + x],
                    pixels[(y + 1) * width + (x + 1)]
                )
                
                // Calculate median for each channel
                val r = neighbors.map { Color.red(it) }.sorted()[4]
                val g = neighbors.map { Color.green(it) }.sorted()[4]
                val b = neighbors.map { Color.blue(it) }.sorted()[4]
                
                resultPixels[index] = Color.rgb(r, g, b)
            }
        }
        
        result.setPixels(resultPixels, 0, width, 0, 0, width, height)
        return result
    }
    
    /**
     * Apply adaptive thresholding
     */
    private fun applyAdaptiveThreshold(bitmap: Bitmap, blockSize: Int, constant: Double): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // Get pixels
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val resultPixels = IntArray(width * height)
        
        // Apply adaptive threshold
        val radius = blockSize / 2
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                val pixelValue = Color.red(pixels[index]) // Use red channel for grayscale
                
                // Calculate local threshold
                var sum = 0
                var count = 0
                
                for (ny in max(0, y - radius) until min(height, y + radius + 1)) {
                    for (nx in max(0, x - radius) until min(width, x + radius + 1)) {
                        sum += Color.red(pixels[ny * width + nx])
                        count++
                    }
                }
                
                val threshold = sum / count - constant
                
                // Apply threshold
                val newValue = if (pixelValue > threshold) 255 else 0
                resultPixels[index] = Color.rgb(newValue, newValue, newValue)
            }
        }
        
        result.setPixels(resultPixels, 0, width, 0, 0, width, height)
        return result
    }
    
    /**
     * Sharpen image
     */
    private fun sharpenImage(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // Get pixels
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val resultPixels = IntArray(width * height)
        
        // Sharpening kernel
        val kernel = arrayOf(
            floatArrayOf(0f, -1f, 0f),
            floatArrayOf(-1f, 5f, -1f),
            floatArrayOf(0f, -1f, 0f)
        )
        
        // Apply convolution
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val index = y * width + x
                
                var r = 0f
                var g = 0f
                var b = 0f
                
                // Apply kernel
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixel = pixels[(y + ky) * width + (x + kx)]
                        val weight = kernel[ky + 1][kx + 1]
                        
                        r += Color.red(pixel) * weight
                        g += Color.green(pixel) * weight
                        b += Color.blue(pixel) * weight
                    }
                }
                
                // Clamp values
                val newR = min(255, max(0, r.toInt()))
                val newG = min(255, max(0, g.toInt()))
                val newB = min(255, max(0, b.toInt()))
                
                resultPixels[index] = Color.rgb(newR, newG, newB)
            }
        }
        
        // Copy edge pixels directly
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                if (y == 0 || x == 0 || y == height - 1 || x == width - 1) {
                    resultPixels[index] = pixels[index]
                }
            }
        }
        
        result.setPixels(resultPixels, 0, width, 0, 0, width, height)
        return result
    }
}
