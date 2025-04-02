package com.example.struku.data.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implementasi OCR menggunakan Google ML Kit
 */
class MlKitOcrEngine @Inject constructor() {
    private val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    /**
     * Menggunakan ML Kit untuk mengenali teks dari gambar
     * @param image Gambar untuk OCR
     * @return Objek Text dari ML Kit yang berisi hasil OCR
     */
    suspend fun recognizeText(image: Bitmap): Text = suspendCancellableCoroutine { continuation ->
        val inputImage = InputImage.fromBitmap(image, 0)
        
        recognizer.process(inputImage)
            .addOnSuccessListener { text ->
                continuation.resume(text)
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
            
        continuation.invokeOnCancellation {
            // No-op, ML Kit doesn't provide cancellation API
        }
    }
    
    /**
     * Convenience method that returns raw text string instead of Text object
     */
    suspend fun recognizeTextAsString(image: Bitmap): String {
        return recognizeText(image).text
    }
    
    /**
     * Get structured text blocks with position information
     */
    suspend fun getTextBlocks(image: Bitmap): List<TextBlock> {
        val text = recognizeText(image)
        return text.textBlocks.map { block ->
            TextBlock(
                text = block.text,
                boundingBox = block.boundingBox,
                cornerPoints = block.cornerPoints,
                lines = block.lines.map { line ->
                    TextLine(
                        text = line.text,
                        boundingBox = line.boundingBox,
                        cornerPoints = line.cornerPoints,
                        elements = line.elements.map { element ->
                            TextElement(
                                text = element.text,
                                boundingBox = element.boundingBox,
                                cornerPoints = element.cornerPoints
                            )
                        }
                    )
                }
            )
        }
    }
}
