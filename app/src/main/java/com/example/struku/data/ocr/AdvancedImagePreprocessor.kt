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
 * Optimized for performance
 */
@Singleton
class AdvancedImagePreprocessor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val visualizer: PreprocessingVisualizer
) {
    private val TAG = "AdvancedImagePreprocessor"
    
    // Cache for intermediate processing steps
    private var preprocessingCache: MutableMap<String, Bitmap> = mutableMapOf()
    
    /**
     * Process a receipt image with the specified configuration
     */
    fun processReceiptImage(bitmap: Bitmap, config: ReceiptPreprocessingConfig): Bitmap {
        // Log start of processing
        Log.d(TAG, "Starting processing of image ${bitmap.width}x${bitmap.height} with level ${config.preprocessingLevel}")
        
        if (!config.enabled) {
            Log.d(TAG, "Processing disabled, returning original image")
            return bitmap
        }
        
        // Generate a cache key for this bitmap and config
        val cacheKey = generateCacheKey(bitmap, config)
        
        // Check if a processed version exists in the cache
        preprocessingCache[cacheKey]?.let {
            Log.d(TAG, "Using cached processed image")
            return it
        }
        
        // Resize image first to optimize processing
        val maxDimension = 1200 // Increased from 800 to improve OCR quality
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
            // Check if the image is a typical receipt format (long vertical format)
            val isReceiptFormat = bitmap.height > bitmap.width * 1.5
            
            // Apply processing based on level (optimized versions)
            processedImage = when (config.preprocessingLevel) {
                PreprocessingLevel.BASIC -> {
                    applyOptimizedBasicPreprocessing(processedImage)
                }
                PreprocessingLevel.STANDARD -> {
                    if (isReceiptFormat) {
                        // Prioritize text clarity for receipt-shaped images
                        applyReceiptOptimizedPreprocessing(processedImage, config)
                    } else {
                        applyOptimizedAdvancedPreprocessing(processedImage, config)
                    }
                }
                PreprocessingLevel.ENHANCED -> {
                    if (isReceiptFormat) {
                        // Prioritize text clarity for receipt-shaped images
                        applyReceiptOptimizedPreprocessing(processedImage, config)
                    } else {
                        applyOptimizedAdvancedPreprocessing(processedImage, config)
                    }
                }
                PreprocessingLevel.ADVANCED -> {
                    if (isReceiptFormat) {
                        // Prioritize text clarity for receipt-shaped images
                        applyReceiptOptimizedPreprocessing(processedImage, config)
                    } else {
                        applyOptimizedAdvancedPreprocessing(processedImage, config)
                    }
                }
                PreprocessingLevel.INTENSIVE -> {
                    applyOptimizedMaximumPreprocessing(processedImage, config)
                }
                PreprocessingLevel.MAXIMUM -> {
                    applyOptimizedMaximumPreprocessing(processedImage, config)
                }
            }
            
            // Cache the result
            preprocessingCache[cacheKey] = processedImage
            Log.d(TAG, "Processed image cached with key: $cacheKey")
            
            // Limit cache size to prevent memory issues
            if (preprocessingCache.size > 3) {
                // Remove oldest entry
                preprocessingCache.entries.firstOrNull()?.let {
                    preprocessingCache.remove(it.key)
                    Log.d(TAG, "Removed oldest cached image: ${it.key}")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during image preprocessing: ${e.message}", e)
            // Return resized bitmap if processing fails
            return resizedBitmap
        }
        
        Log.d(TAG, "Image processing complete")
        return processedImage
    }
    
    /**
     * Generate a unique cache key for a bitmap and config
     */
    private fun generateCacheKey(bitmap: Bitmap, config: ReceiptPreprocessingConfig): String {
        return "${bitmap.width}x${bitmap.height}_${config.preprocessingLevel}_${System.identityHashCode(bitmap)}"
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
        
        Log.d(TAG, "Resizing image from ${width}x${height} to ${newWidth}x${newHeight}")
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * Apply optimized basic preprocessing (grayscale, contrast)
     */
    private fun applyOptimizedBasicPreprocessing(bitmap: Bitmap): Bitmap {
        // Convert to grayscale (optimized)
        val grayscaleBitmap = toGrayscaleFast(bitmap)
        visualizer.captureProcessingStep(
            "Grayscale",
            "Converted to grayscale",
            grayscaleBitmap
        )
        
        // Increase contrast (optimized)
        val contrastBitmap = adjustContrastFast(grayscaleBitmap, 1.3f) // Reduced contrast level
        visualizer.captureProcessingStep(
            "Contrast Enhanced",
            "Increased contrast for better text visibility",
            contrastBitmap
        )
        
        return contrastBitmap
    }
    
    /**
     * Apply preprocessing optimized specifically for receipt formats with item lists
     */
    private fun applyReceiptOptimizedPreprocessing(bitmap: Bitmap, config: ReceiptPreprocessingConfig): Bitmap {
        // First apply basic preprocessing
        var processedImage = applyOptimizedBasicPreprocessing(bitmap)
        
        // Apply specific brightness adjustment to make text clearer
        processedImage = adjustBrightness(processedImage, 10f)
        visualizer.captureProcessingStep(
            "Brightness Adjusted",
            "Optimized brightness for receipt text",
            processedImage
        )
        
        // Apply a moderate sharpening to improve text edges
        processedImage = applySharpening(processedImage, 1.2f)
        visualizer.captureProcessingStep(
            "Sharpened",
            "Applied sharpening to improve text clarity",
            processedImage
        )
        
        // Apply noise reduction that preserves text details
        processedImage = applyNoiseReductionForText(processedImage)
        visualizer.captureProcessingStep(
            "Text-optimized Noise Reduction",
            "Applied noise reduction optimized for text",
            processedImage
        )
        
        // Apply receipt-optimized thresholding
        processedImage = applyReceiptOptimizedThreshold(processedImage)
        visualizer.captureProcessingStep(
            "Receipt-optimized Threshold",
            "Applied thresholding optimized for item lists",
            processedImage
        )
        
        return processedImage
    }
    
    /**
     * Apply thresholding optimized for receipt-style text with numbers and columns
     */
    private fun applyReceiptOptimizedThreshold(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // Get pixels
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val resultPixels = IntArray(width * height)
        
        // Use a smaller block size for finer details
        val blockSize = 15
        val radius = blockSize / 2
        val constant = 5.0 // Lower constant to better preserve text
        
        // Apply adaptive threshold with full processing (no skip)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val blockAvg = calculateOptimizedBlockAverage(pixels, width, height, x, y, radius)
                val threshold = blockAvg - constant
                
                val index = y * width + x
                val pixelValue = Color.red(pixels[index])
                val newValue = if (pixelValue > threshold) 255 else 0
                resultPixels[index] = Color.rgb(newValue, newValue, newValue)
            }
        }
        
        result.setPixels(resultPixels, 0, width, 0, 0, width, height)
        return result
    }
    
    /**
     * Apply noise reduction optimized for preserving text details
     */
    private fun applyNoiseReductionForText(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // Use a small radius to preserve text details
        return applyFastBlur(bitmap, 1)
    }
    
    /**
     * Apply sharpening to emphasize text edges
     */
    private fun applySharpening(bitmap: Bitmap, amount: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // Define a simple sharpening convolution matrix
        val matrix = floatArrayOf(
            0f, -amount, 0f,
            -amount, 1 + 4 * amount, -amount,
            0f, -amount, 0f
        )
        
        // Apply the convolution - optimized version that skips edges
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val resultPixels = IntArray(width * height)
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var red = 0f
                var green = 0f
                var blue = 0f
                
                // Apply convolution
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixel = pixels[(y + ky) * width + (x + kx)]
                        val k = matrix[(ky + 1) * 3 + (kx + 1)]
                        
                        red += Color.red(pixel) * k
                        green += Color.green(pixel) * k
                        blue += Color.blue(pixel) * k
                    }
                }
                
                // Clamp values to valid range
                red = min(255f, max(0f, red))
                green = min(255f, max(0f, green))
                blue = min(255f, max(0f, blue))
                
                resultPixels[y * width + x] = Color.rgb(red.toInt(), green.toInt(), blue.toInt())
            }
        }
        
        // Copy edge pixels unchanged
        for (y in 0 until height) {
            resultPixels[y * width] = pixels[y * width]
            resultPixels[y * width + width - 1] = pixels[y * width + width - 1]
        }
        for (x in 0 until width) {
            resultPixels[x] = pixels[x]
            resultPixels[(height - 1) * width + x] = pixels[(height - 1) * width + x]
        }
        
        result.setPixels(resultPixels, 0, width, 0, 0, width, height)
        return result
    }
    
    /**
     * Adjust image brightness
     */
    private fun adjustBrightness(bitmap: Bitmap, brightness: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val canvas = Canvas(result)
        val paint = Paint()
        
        // Create color matrix to adjust brightness
        val colorMatrix = ColorMatrix()
        colorMatrix.set(floatArrayOf(
            1f, 0f, 0f, 0f, brightness,
            0f, 1f, 0f, 0f, brightness,
            0f, 0f, 1f, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))
        
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return result
    }
    
    /**
     * Apply optimized advanced preprocessing (grayscale, contrast, noise reduction)
     */
    private fun applyOptimizedAdvancedPreprocessing(bitmap: Bitmap, config: ReceiptPreprocessingConfig): Bitmap {
        // First apply basic preprocessing
        var processedImage = applyOptimizedBasicPreprocessing(bitmap)
        
        // Apply a faster noise reduction (using optimized algorithm)
        processedImage = applyOptimizedNoiseReduction(processedImage)
        visualizer.captureProcessingStep(
            "Noise Reduction",
            "Applied noise reduction filter",
            processedImage
        )
        
        // Apply faster adaptive thresholding based on receipt type
        when (config.receiptType) {
            ReceiptType.THERMAL -> {
                processedImage = applyOptimizedAdaptiveThreshold(processedImage, 12, 2.0)
            }
            ReceiptType.INKJET, ReceiptType.LASER -> {
                processedImage = applyOptimizedAdaptiveThreshold(processedImage, 8, 1.0)
            }
            else -> {
                processedImage = applyOptimizedAdaptiveThreshold(processedImage, 12, 1.5)
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
     * Apply optimized maximum preprocessing (everything + sharpening)
     */
    private fun applyOptimizedMaximumPreprocessing(bitmap: Bitmap, config: ReceiptPreprocessingConfig): Bitmap {
        // First apply advanced preprocessing
        var processedImage = applyOptimizedAdvancedPreprocessing(bitmap, config)
        
        // For thermal receipts, apply additional contrast
        if (config.receiptType == ReceiptType.THERMAL) {
            processedImage = adjustContrastFast(processedImage, 1.5f)
            visualizer.captureProcessingStep(
                "Extra Contrast",
                "Applied additional contrast for thermal paper",
                processedImage
            )
        }
        
        // Apply sharpening for improved OCR accuracy
        processedImage = applySharpening(processedImage, 1.0f)
        visualizer.captureProcessingStep(
            "Sharpened",
            "Applied sharpening for improved OCR clarity",
            processedImage
        )
        
        return processedImage
    }
    
    /**
     * Convert to grayscale (optimized version)
     */
    private fun toGrayscaleFast(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val canvas = Canvas(result)
        val paint = Paint()
        
        // Apply optimized grayscale color filter
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f) // 0 = grayscale
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }
    
    /**
     * Adjust contrast (optimized version)
     */
    private fun adjustContrastFast(bitmap: Bitmap, contrast: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val canvas = Canvas(result)
        val paint = Paint()
        
        // Apply contrast matrix (optimized)
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
     * Apply optimized noise reduction (faster than previous implementation)
     */
    private fun applyOptimizedNoiseReduction(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // For very large images, use a more aggressive downsampling
        if (width * height > 800000) {
            val downsampled = Bitmap.createScaledBitmap(bitmap, width/2, height/2, true)
            val blurred = applyFastBlur(downsampled, 2)
            return Bitmap.createScaledBitmap(blurred, width, height, true)
        } else {
            return applyFastBlur(bitmap, 2)
        }
    }
    
    /**
     * Apply a fast gaussian-like blur for noise reduction
     */
    private fun applyFastBlur(bitmap: Bitmap, radius: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val result = Bitmap.createBitmap(w, h, bitmap.config)
        
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)
        
        // Blur horizontally
        for (r in 0 until h) {
            for (c in 0 until w) {
                var sumR = 0
                var sumG = 0
                var sumB = 0
                var count = 0
                
                for (i in -radius..radius) {
                    val col = c + i
                    if (col >= 0 && col < w) {
                        val pixel = pix[r * w + col]
                        sumR += Color.red(pixel)
                        sumG += Color.green(pixel)
                        sumB += Color.blue(pixel)
                        count++
                    }
                }
                
                val avgR = sumR / count
                val avgG = sumG / count
                val avgB = sumB / count
                
                pix[r * w + c] = Color.rgb(avgR, avgG, avgB)
            }
        }
        
        // Create temporary array for vertical blur
        val temp = IntArray(w * h)
        System.arraycopy(pix, 0, temp, 0, pix.size)
        
        // Blur vertically
        for (c in 0 until w) {
            for (r in 0 until h) {
                var sumR = 0
                var sumG = 0
                var sumB = 0
                var count = 0
                
                for (i in -radius..radius) {
                    val row = r + i
                    if (row >= 0 && row < h) {
                        val pixel = temp[row * w + c]
                        sumR += Color.red(pixel)
                        sumG += Color.green(pixel)
                        sumB += Color.blue(pixel)
                        count++
                    }
                }
                
                val avgR = sumR / count
                val avgG = sumG / count
                val avgB = sumB / count
                
                pix[r * w + c] = Color.rgb(avgR, avgG, avgB)
            }
        }
        
        result.setPixels(pix, 0, w, 0, 0, w, h)
        return result
    }
    
    /**
     * Apply optimized adaptive threshold (faster implementation)
     */
    private fun applyOptimizedAdaptiveThreshold(bitmap: Bitmap, blockSize: Int, constant: Double): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // Get pixels
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val resultPixels = IntArray(width * height)
        
        // For large images, use a more aggressive downsampling
        val skipFactor = if (width * height > 800000) 4 else 2
        
        // Process image with skipping
        val radius = blockSize / 2
        
        // Process image with skipping for better performance
        for (y in 0 until height step skipFactor) {
            for (x in 0 until width step skipFactor) {
                val blockAvg = calculateOptimizedBlockAverage(pixels, width, height, x, y, radius)
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
     * Calculate average pixel value in a block - highly optimized version
     */
    private fun calculateOptimizedBlockAverage(pixels: IntArray, width: Int, height: Int, centerX: Int, centerY: Int, radius: Int): Double {
        var sum = 0
        var count = 0
        
        // Use larger steps for big blocks to improve performance
        val sampleStep = if (radius > 4) 3 else 2
        
        for (y in max(0, centerY - radius) until min(height, centerY + radius + 1) step sampleStep) {
            for (x in max(0, centerX - radius) until min(width, centerX + radius + 1) step sampleStep) {
                sum += Color.red(pixels[y * width + x])
                count++
            }
        }
        
        return if (count > 0) sum.toDouble() / count else 0.0
    }
    
    /**
     * Clear any cached preprocessing results
     */
    fun clearCache() {
        val cacheSize = preprocessingCache.size
        preprocessingCache.clear()
        Log.d(TAG, "Cleared image preprocessing cache ($cacheSize entries)")
    }
}