package com.example.struku.data.ocr

/**
 * Configuration for receipt preprocessing
 */
data class ReceiptPreprocessingConfig(
    val enabled: Boolean = true,
    val receiptType: ReceiptType = ReceiptType.AUTO_DETECT,
    val preprocessingLevel: PreprocessingLevel = PreprocessingLevel.STANDARD,
    val documentDetectionConfig: DocumentDetectionConfig = DocumentDetectionConfig(),
    val layoutAnalysisConfig: LayoutAnalysisConfig = LayoutAnalysisConfig(),
    val imageEnhancementConfig: ImageEnhancementConfig = ImageEnhancementConfig()
)

/**
 * Types of receipts that may require different preprocessing
 */
enum class ReceiptType {
    AUTO_DETECT,  // Auto-detect based on image characteristics
    THERMAL,      // Thermal printer receipts (common in stores)
    INKJET,       // Inkjet printed receipts
    LASER,        // Laser printed receipts (higher quality)
    DIGITAL       // Digital receipts (screenshots, PDFs)
}

/**
 * Preprocessing intensity levels
 */
enum class PreprocessingLevel {
    BASIC,       // Minimal processing for speed
    STANDARD,    // Balanced processing
    MAXIMUM      // Maximum quality processing (slower)
}

/**
 * Document detection and geometric correction configuration
 */
data class DocumentDetectionConfig(
    val enableAutoDetect: Boolean = true,
    val enablePerspectiveCorrection: Boolean = true,
    val enableCropping: Boolean = true,
    val detectionSensitivity: Double = 0.5,
    val boundaryMargin: Int = 5
)

/**
 * Layout analysis configuration
 */
data class LayoutAnalysisConfig(
    val enableStructuralAnalysis: Boolean = true,
    val enableTextlineDetection: Boolean = true,
    val enableSkewCorrection: Boolean = true,
    val detectTables: Boolean = true,
    val detectSeparators: Boolean = true
)

/**
 * Image enhancement configuration
 */
data class ImageEnhancementConfig(
    val contrastEnhancement: Boolean = true,
    val contrastLevel: Double = 0.5,
    val denoise: Boolean = true,
    val denoiseStrength: Double = 0.5,
    val enableBinarization: Boolean = true,
    val thresholdingMethod: ThresholdingMethod = ThresholdingMethod.ADAPTIVE,
    val adaptiveBlockSize: Int = 11,
    val adaptiveConstant: Double = 2.0,
    val enableMorphologicalOperations: Boolean = true,
    val enableSharpening: Boolean = true,
    val sharpeningIntensity: Double = 0.5
)

/**
 * Thresholding methods for binarization
 */
enum class ThresholdingMethod {
    GLOBAL,      // Global threshold
    ADAPTIVE,    // Adaptive threshold
    OTSU,        // Otsu's method
    SAUVOLA      // Sauvola's method (good for documents)
}

/**
 * Factory to create commonly used preprocessing configs
 */
object ReceiptPreprocessingConfigFactory {
    
    /**
     * Config optimized for thermal receipts
     */
    fun thermalReceiptConfig(): ReceiptPreprocessingConfig {
        return ReceiptPreprocessingConfig(
            receiptType = ReceiptType.THERMAL,
            preprocessingLevel = PreprocessingLevel.STANDARD,
            imageEnhancementConfig = ImageEnhancementConfig(
                contrastLevel = 0.7,
                enableBinarization = true,
                thresholdingMethod = ThresholdingMethod.ADAPTIVE,
                adaptiveBlockSize = 15,
                adaptiveConstant = 5.0,
                enableSharpening = true,
                sharpeningIntensity = 0.7
            )
        )
    }
    
    /**
     * Config optimized for formal printed receipts (inkjet/laser)
     */
    fun formalReceiptConfig(): ReceiptPreprocessingConfig {
        return ReceiptPreprocessingConfig(
            receiptType = ReceiptType.LASER,
            preprocessingLevel = PreprocessingLevel.STANDARD,
            imageEnhancementConfig = ImageEnhancementConfig(
                denoiseStrength = 0.3,
                enableBinarization = true,
                thresholdingMethod = ThresholdingMethod.ADAPTIVE,
                adaptiveBlockSize = 9,
                adaptiveConstant = 2.0
            )
        )
    }
    
    /**
     * Config optimized for speed
     */
    fun speedOptimizedConfig(): ReceiptPreprocessingConfig {
        return ReceiptPreprocessingConfig(
            preprocessingLevel = PreprocessingLevel.BASIC,
            layoutAnalysisConfig = LayoutAnalysisConfig(
                enableStructuralAnalysis = false,
                enableTextlineDetection = true,
                enableSkewCorrection = true,
                detectTables = false,
                detectSeparators = false
            ),
            imageEnhancementConfig = ImageEnhancementConfig(
                denoise = false,
                enableMorphologicalOperations = false,
                enableSharpening = false
            )
        )
    }
    
    /**
     * Config optimized for quality
     */
    fun qualityOptimizedConfig(): ReceiptPreprocessingConfig {
        return ReceiptPreprocessingConfig(
            preprocessingLevel = PreprocessingLevel.MAXIMUM,
            documentDetectionConfig = DocumentDetectionConfig(
                detectionSensitivity = 0.7,
                boundaryMargin = 2
            ),
            layoutAnalysisConfig = LayoutAnalysisConfig(
                enableStructuralAnalysis = true,
                enableTextlineDetection = true,
                enableSkewCorrection = true,
                detectTables = true,
                detectSeparators = true
            ),
            imageEnhancementConfig = ImageEnhancementConfig(
                contrastEnhancement = true,
                contrastLevel = 0.6,
                denoise = true,
                denoiseStrength = 0.6,
                enableBinarization = true,
                thresholdingMethod = ThresholdingMethod.ADAPTIVE,
                adaptiveBlockSize = 13,
                adaptiveConstant = 3.0,
                enableMorphologicalOperations = true,
                enableSharpening = true,
                sharpeningIntensity = 0.6
            )
        )
    }
}