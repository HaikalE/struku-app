package com.example.struku.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * OCR Engine using ML Kit's text recognition
 * Optimized with model caching and improved text handling
 */
@Singleton
class MlKitOcrEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "MlKitOcrEngine"
    
    // Create text recognizer with singleton instance
    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    
    // Cache for recent OCR results to avoid duplicate processing
    private val resultCache = LruCache<String, String>(5)
    
    /**
     * Recognize text in an image
     *
     * @param bitmap Image to process
     * @return Raw recognized text
     */
    suspend fun recognizeText(bitmap: Bitmap): String = withContext(Dispatchers.Default) {
        try {
            // Generate cache key based on image bitmap hash
            val cacheKey = bitmap.hashCode().toString()
            
            // Check cache first
            resultCache.get(cacheKey)?.let {
                Log.d(TAG, "Using cached OCR result")
                return@withContext it
            }
            
            // Not in cache, process image
            val result = processImageWithOcr(bitmap)
            
            // Cache the result
            resultCache.put(cacheKey, result)
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error during text recognition: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Process image with OCR
     */
    private suspend fun processImageWithOcr(bitmap: Bitmap): String = suspendCancellableCoroutine { continuation ->
        try {
            // Create input image
            val image = InputImage.fromBitmap(bitmap, 0)
            
            // Process image
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val text = processVisionText(visionText)
                    continuation.resume(text)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Text recognition failed: ${e.message}", e)
                    continuation.resumeWithException(e)
                }
                
            continuation.invokeOnCancellation {
                // No cleanup needed here
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image: ${e.message}", e)
            continuation.resumeWithException(e)
        }
    }
    
    /**
     * Process detected text from ML Kit with improved formatting
     */
    private fun processVisionText(visionText: Text): String {
        val builder = StringBuilder()
        
        // Process each text block with better formatting
        for (block in visionText.textBlocks) {
            // Skip very short blocks that might be noise
            if (block.text.length < 2) continue
            
            // Process each line
            for (line in block.lines) {
                // Skip very short lines
                if (line.text.length < 2) continue
                
                builder.append(line.text.trim()).append("\n")
            }
            
            // Add extra space between blocks for better readability
            builder.append("\n")
        }
        
        return builder.toString().trim()
    }
    
    /**
     * Extract text blocks for detailed processing
     */
    suspend fun extractTextBlocks(bitmap: Bitmap): List<TextBlock> = suspendCancellableCoroutine { continuation ->
        try {
            // Create input image
            val image = InputImage.fromBitmap(bitmap, 0)
            
            // Process image
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val textBlocks = mapVisionTextToBlocks(visionText)
                    continuation.resume(textBlocks)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Text block extraction failed: ${e.message}", e)
                    continuation.resumeWithException(e)
                }
                
            continuation.invokeOnCancellation {
                // No cleanup needed here
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image for text blocks: ${e.message}", e)
            continuation.resumeWithException(e)
        }
    }
    
    /**
     * Map ML Kit vision text to our domain models
     */
    private fun mapVisionTextToBlocks(visionText: Text): List<TextBlock> {
        return visionText.textBlocks.map { block ->
            val lines = block.lines.map { line ->
                val elements = line.elements.map { element ->
                    TextElement(
                        text = element.text,
                        boundingBox = element.boundingBox,
                        cornerPoints = element.cornerPoints
                    )
                }
                
                TextLine(
                    text = line.text,
                    boundingBox = line.boundingBox,
                    cornerPoints = line.cornerPoints,
                    elements = elements
                )
            }
            
            TextBlock(
                text = block.text,
                boundingBox = block.boundingBox,
                cornerPoints = block.cornerPoints,
                lines = lines
            )
        }
    }
    
    /**
     * Clear all cached results
     */
    fun clearCache() {
        resultCache.evictAll()
    }
}

/**
 * Simple LRU cache implementation
 */
class LruCache<K, V>(private val maxSize: Int) {
    private val map = LinkedHashMap<K, V>(maxSize, 0.75f, true)
    
    @Synchronized
    fun put(key: K, value: V) {
        map[key] = value
        trimCache()
    }
    
    @Synchronized
    fun get(key: K): V? {
        return map[key]
    }
    
    @Synchronized
    fun evictAll() {
        map.clear()
    }
    
    private fun trimCache() {
        while (map.size > maxSize) {
            val firstKey = map.keys.firstOrNull() ?: break
            map.remove(firstKey)
        }
    }
}
