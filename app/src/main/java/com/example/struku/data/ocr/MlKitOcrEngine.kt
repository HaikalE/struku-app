package com.example.struku.data.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implementasi OCR menggunakan Google ML Kit dengan advanced preprocessing
 */
@Singleton
class MlKitOcrEngine @Inject constructor(
    private val imagePreprocessor: AdvancedImagePreprocessor
) {
    private val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    // Status OCR
    private val _ocrState = MutableStateFlow<OcrState>(OcrState.Idle)
    val ocrState: StateFlow<OcrState> = _ocrState.asStateFlow()
    
    /**
     * State untuk pelacakan proses OCR
     */
    sealed class OcrState {
        object Idle : OcrState()
        class InProgress(val stage: String, val progress: Float) : OcrState()
        class Success(val text: Text) : OcrState()
        class Error(val message: String, val exception: Exception? = null) : OcrState()
    }
    
    /**
     * Menggunakan ML Kit untuk mengenali teks dari gambar dengan preprocessing
     * @param image Gambar untuk OCR
     * @param config Konfigurasi pre-processing
     * @return Objek Text dari ML Kit yang berisi hasil OCR
     */
    suspend fun recognizeText(
        image: Bitmap,
        config: ReceiptPreprocessingConfig = ReceiptPreprocessingConfig()
    ): Text {
        try {
            _ocrState.value = OcrState.InProgress("Preprocessing image", 0.0f)
            
            // 1. Pre-process gambar
            val preprocessedImage = if (config.enabled) {
                imagePreprocessor.processReceiptImage(image, config)
            } else {
                image
            }
            
            // 2. Lakukan OCR
            _ocrState.value = OcrState.InProgress("Performing OCR", 0.5f)
            val result = performOcr(preprocessedImage)
            
            _ocrState.value = OcrState.Success(result)
            return result
            
        } catch (e: Exception) {
            _ocrState.value = OcrState.Error("OCR failed: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Melakukan OCR dengan ML Kit
     */
    private suspend fun performOcr(image: Bitmap): Text = suspendCancellableCoroutine { continuation ->
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
    suspend fun recognizeTextAsString(
        image: Bitmap,
        config: ReceiptPreprocessingConfig = ReceiptPreprocessingConfig()
    ): String {
        return recognizeText(image, config).text
    }
    
    /**
     * Get structured text blocks with position information
     */
    suspend fun getTextBlocks(
        image: Bitmap,
        config: ReceiptPreprocessingConfig = ReceiptPreprocessingConfig()
    ): List<TextBlock> {
        val text = recognizeText(image, config)
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
    
    /**
     * Mengenali teks dengan optimasi terbaik untuk jenis struk tertentu
     */
    suspend fun recognizeTextOptimizedFor(
        image: Bitmap,
        receiptType: ReceiptType
    ): Text {
        val config = when (receiptType) {
            ReceiptType.THERMAL -> ReceiptPreprocessingConfigFactory.thermalReceiptConfig()
            ReceiptType.INKJET, ReceiptType.LASER -> ReceiptPreprocessingConfigFactory.formalReceiptConfig()
            ReceiptType.DIGITAL -> ReceiptPreprocessingConfig(receiptType = ReceiptType.DIGITAL)
            else -> ReceiptPreprocessingConfig() // Auto-detect
        }
        
        return recognizeText(image, config)
    }
    
    /**
     * Mengenali teks dengan optimasi kecepatan
     */
    suspend fun recognizeTextFast(image: Bitmap): Text {
        return recognizeText(image, ReceiptPreprocessingConfigFactory.speedOptimizedConfig())
    }
    
    /**
     * Mengenali teks dengan optimasi kualitas maksimum (lebih lambat)
     */
    suspend fun recognizeTextHighQuality(image: Bitmap): Text {
        return recognizeText(image, ReceiptPreprocessingConfigFactory.qualityOptimizedConfig())
    }
}