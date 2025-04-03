package com.example.struku.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * OCR Engine using ML Kit's text recognition
 */
@Singleton
class MlKitOcrEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imagePreprocessor: AdvancedImagePreprocessor
) {
    private val TAG = "MlKitOcrEngine"
    
    // Create text recognizer
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    /**
     * Recognize text in an image
     *
     * @param bitmap Image to process
     * @return Raw recognized text
     */
    suspend fun recognizeText(bitmap: Bitmap): String = suspendCancellableCoroutine { continuation ->
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
     * Process detected text from ML Kit
     */
    private fun processVisionText(visionText: Text): String {
        val builder = StringBuilder()
        
        // Process each text block
        for (block in visionText.textBlocks) {
            // Process each line
            for (line in block.lines) {
                builder.append(line.text).append("\n")
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
}