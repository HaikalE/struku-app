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
        
        // Resize image first to optimize processing
        val maxDimension = 1200 // Reduced from original 8MP resolution
        val resizedBitmap = resizeImageIfNeeded(bitmap, maxDimension)
        
        // Start with a copy of the resized image
        var processedImage = resizedBitmap.copy(resizedBitmap.config, true)
        
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
            // Return resized bitmap if processing fails
            return resizedBitmap
        }
        
        return processedImage
    }
    
    /**
     * Resize image if it exceeds the maximum dimension
     */
    private fun resizeImageIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }
        
        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        
        if (width > height) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt()
        }
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
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
        
        // Apply a faster noise reduction (using simpler algorithm)
        processedImage = applyFastNoiseReduction(processedImage)
        visualizer.captureProcessingStep(
            "Noise Reduction",
            "Applied noise reduction filter",
            processedImage
        )
        
        // Apply faster adaptive thresholding based on receipt type
        when (config.receiptType) {
            ReceiptType.THERMAL -> {
                processedImage = applyFastAdaptiveThreshold(processedImage, 8, 2.0)
            }
            ReceiptType.INKJET, ReceiptType.LASER -> {
                processedImage = applyFastAdaptiveThreshold(processedImage, 5, 1.0)
            }
            else -> {
                processedImage = applyFastAdaptiveThreshold(processedImage, 8, 1.5)
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
     * Apply faster noise reduction (simple blur)
     */
    private fun applyFastNoiseReduction(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val result = IntArray(width * height)
        
        // Simple blur - just average of surrounding pixels (faster than median filter)
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                
                // Get average of 3x3 pixels
                var sumR = 0
                var sumG = 0
                var sumB = 0
                
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val pixel = pixels[(y + dy) * width + (x + dx)]
                        sumR += Color.red(pixel)
                        sumG += Color.green(pixel)
                        sumB += Color.blue(pixel)
                    }
                }
                
                val avgR = sumR / 9
                val avgG = sumG / 9
                val avgB = sumB / 9
                
                result[idx] = Color.rgb(avgR, avgG, avgB)
            }
        }
        
        // Copy edges
        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                if (y == 0 || x == 0 || y == height - 1 || x == width - 1) {
                    result[idx] = pixels[idx]
                }
            }
        }
        
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(result, 0, width, 0, 0, width, height)
        return resultBitmap
    }
    
    /**
     * Apply faster adaptive threshold with downsampling
     */
    private fun applyFastAdaptiveThreshold(bitmap: Bitmap, blockSize: Int, constant: Double): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // Get pixels
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val resultPixels = IntArray(width * height)
        
        // Skip pixels to improve speed - process every 2nd pixel
        val radius = blockSize / 2
        val skipFactor = 2
        
        // Process image with skipping
        for (y in 0 until height step skipFactor) {
            for (x in 0 until width step skipFactor) {
                val blockAvg = calculateBlockAverage(pixels, width, height, x, y, radius)
                val threshold = blockAvg - constant
                
                // Apply the same threshold to the skipFactor x skipFactor block
                for (dy in 0 until skipFactor) {
                    if (y + dy >= height) continue
                    for (dx in 0 until skipFactor) {
                        if (x + dx >= width) continue
                        
                        val index = (y + dy) * width + (x + dx)
                        val pixelValue = Color.red(pixels[index])
                        val newValue = if (pixelValue > threshold) 255 else 0
                        resultPixels[index] = Color.rgb(newValue, newValue, newValue)
                    }
                }
            }
        }
        
        result.setPixels(resultPixels, 0, width, 0, 0, width, height)
        return result
    }
    
    /**
     * Calculate average pixel value in a block - optimized version
     */
    private fun calculateBlockAverage(pixels: IntArray, width: Int, height: Int, centerX: Int, centerY: Int, radius: Int): Double {
        var sum = 0
        var count = 0
        
        // Sample fewer pixels from the block to improve speed
        val sampleStep = 2
        
        for (y in max(0, centerY - radius) until min(height, centerY + radius + 1) step sampleStep) {
            for (x in max(0, centerX - radius) until min(width, centerX + radius + 1) step sampleStep) {
                sum += Color.red(pixels[y * width + x])
                count++
            }
        }
        
        return if (count > 0) sum.toDouble() / count else 0.0
    }
    
    /**
     * Sharpen image
     */
    private fun sharpenImage(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // Skip sharpening for large images to save time
        if (width * height > 1000000) {
            return bitmap
        }
        
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
