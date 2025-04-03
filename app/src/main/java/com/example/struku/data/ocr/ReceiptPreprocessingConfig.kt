package com.example.struku.data.ocr

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Konfigurasi untuk pipeline pre-processing struk
 * Konfigurasi ini memungkinkan penyesuaian pipeline berdasarkan jenis struk dan kondisi gambar
 */
@Parcelize
data class ReceiptPreprocessingConfig(
    // Konfigurasi umum
    val enabled: Boolean = true,
    val receiptType: ReceiptType = ReceiptType.AUTO_DETECT,
    val preprocessingLevel: PreprocessingLevel = PreprocessingLevel.ADVANCED,
    val qualityPreference: QualityPreference = QualityPreference.BALANCED,
    
    // Konfigurasi spesifik untuk akuisisi gambar
    val captureConfig: CaptureConfig = CaptureConfig(),
    
    // Konfigurasi spesifik untuk deteksi dokumen
    val documentDetectionConfig: DocumentDetectionConfig = DocumentDetectionConfig(),
    
    // Konfigurasi untuk peningkatan gambar
    val imageEnhancementConfig: ImageEnhancementConfig = ImageEnhancementConfig(),
    
    // Konfigurasi untuk analisis tata letak
    val layoutAnalysisConfig: LayoutAnalysisConfig = LayoutAnalysisConfig(),
    
    // Konfigurasi untuk spesifik perangkat
    val deviceOptimizationConfig: DeviceOptimizationConfig = DeviceOptimizationConfig()
) : Parcelable

/**
 * Jenis struk yang akan diproses
 */
enum class ReceiptType {
    THERMAL, // Struk kasir dengan kertas termal (paling umum)
    INKJET,  // Struk dengan tinta inkjet (e.g., beberapa struk formal)
    LASER,   // Struk dengan tinta laser (e.g., invoice formal)
    DIGITAL, // Screenshot struk digital
    AUTO_DETECT // Otomatis mendeteksi jenis struk
}

/**
 * Level pre-processing yang akan diterapkan
 */
enum class PreprocessingLevel {
    BASIC,     // Pre-processing dasar (lebih cepat, kurang akurat)
    ADVANCED,  // Pre-processing menengah (seimbang)
    MAXIMUM    // Pre-processing maksimum (lebih lambat, lebih akurat)
}

/**
 * Preferensi kualitas vs kecepatan
 */
enum class QualityPreference {
    SPEED,    // Prioritaskan kecepatan
    BALANCED, // Seimbang antara kecepatan dan kualitas
    QUALITY   // Prioritaskan kualitas
}

/**
 * Konfigurasi untuk fase akuisisi gambar
 */
@Parcelize
data class CaptureConfig(
    val enableHdr: Boolean = true,
    val enableBurstCapture: Boolean = true,
    val burstCount: Int = 3,
    val enableStabilization: Boolean = true,
    val timeout: Long = 10000 // timeout dalam ms
) : Parcelable

/**
 * Konfigurasi untuk fase deteksi dokumen
 */
@Parcelize
data class DocumentDetectionConfig(
    val enableMlKitDocumentScanner: Boolean = true,
    val enableOpenCvDetection: Boolean = true,
    val cornerDetectionSensitivity: Float = 0.5f, // 0.0f - 1.0f
    val perspectiveCorrection: Boolean = true,
    val autoCrop: Boolean = true,
    val cropPadding: Float = 0.02f // Sebagai persentase dari ukuran gambar
) : Parcelable

/**
 * Konfigurasi untuk fase peningkatan gambar
 */
@Parcelize
data class ImageEnhancementConfig(
    val enableGrayscaleConversion: Boolean = true,
    val grayscaleWeights: Triple<Float, Float, Float> = Triple(0.299f, 0.587f, 0.114f), // R, G, B weights
    val enableContrastEnhancement: Boolean = true,
    val contrastMethod: ContrastMethod = ContrastMethod.CLAHE,
    val enableNoiseReduction: Boolean = true,
    val noiseReductionMethod: NoiseReductionMethod = NoiseReductionMethod.ADAPTIVE,
    val enableThresholding: Boolean = true,
    val thresholdingMethod: ThresholdingMethod = ThresholdingMethod.ADAPTIVE_SAUVOLA,
    val enableSharpening: Boolean = true,
    val sharpeningIntensity: Float = 0.5f, // 0.0f - 1.0f
    val enableMorphologicalOperations: Boolean = true
) : Parcelable

/**
 * Metode peningkatan kontras
 */
enum class ContrastMethod {
    HISTOGRAM_EQUALIZATION,
    CLAHE, // Contrast Limited Adaptive Histogram Equalization
    NORMALIZATION
}

/**
 * Metode pengurangan noise
 */
enum class NoiseReductionMethod {
    GAUSSIAN,
    BILATERAL,
    MEDIAN,
    NON_LOCAL_MEANS,
    ADAPTIVE // Kombinasi beberapa metode berdasarkan karakteristik gambar
}

/**
 * Metode thresholding
 */
enum class ThresholdingMethod {
    GLOBAL_OTSU,
    ADAPTIVE_GAUSSIAN,
    ADAPTIVE_MEAN,
    ADAPTIVE_SAUVOLA,
    ADAPTIVE_NIBLACK,
    MULTI_LEVEL // Kombinasi beberapa metode
}

/**
 * Konfigurasi untuk analisis tata letak
 */
@Parcelize
data class LayoutAnalysisConfig(
    val enableLineSegmentation: Boolean = true,
    val enableSkewCorrection: Boolean = true,
    val enableColumnDetection: Boolean = true,
    val enableTableStructureRecognition: Boolean = true,
    val enableTextOrientation: Boolean = true,
    val enableStructuralAnalysis: Boolean = true
) : Parcelable

/**
 * Konfigurasi untuk optimasi perangkat
 */
@Parcelize
data class DeviceOptimizationConfig(
    val enableGpuAcceleration: Boolean = true,
    val enableMultiThreading: Boolean = true,
    val threadCount: Int = 0, // 0 = auto detect based on CPU cores
    val enableLowMemoryMode: Boolean = false,
    val enableProgressiveProcessing: Boolean = true
) : Parcelable

/**
 * Factory untuk membuat konfigurasi standar yang optimum untuk jenis struk tertentu
 */
object ReceiptPreprocessingConfigFactory {
    
    /**
     * Konfigurasi optimal untuk struk termal (struk kasir)
     */
    fun thermalReceiptConfig(): ReceiptPreprocessingConfig {
        return ReceiptPreprocessingConfig(
            receiptType = ReceiptType.THERMAL,
            imageEnhancementConfig = ImageEnhancementConfig(
                contrastMethod = ContrastMethod.CLAHE,
                thresholdingMethod = ThresholdingMethod.ADAPTIVE_SAUVOLA,
                sharpeningIntensity = 0.6f
            )
        )
    }
    
    /**
     * Konfigurasi optimal untuk struk formal (inkjet/laser)
     */
    fun formalReceiptConfig(): ReceiptPreprocessingConfig {
        return ReceiptPreprocessingConfig(
            receiptType = ReceiptType.INKJET,
            imageEnhancementConfig = ImageEnhancementConfig(
                contrastMethod = ContrastMethod.NORMALIZATION,
                noiseReductionMethod = NoiseReductionMethod.BILATERAL,
                thresholdingMethod = ThresholdingMethod.ADAPTIVE_GAUSSIAN
            ),
            layoutAnalysisConfig = LayoutAnalysisConfig(
                enableTableStructureRecognition = true
            )
        )
    }
    
    /**
     * Konfigurasi untuk kecepatan maksimum (kualitas lebih rendah)
     */
    fun speedOptimizedConfig(): ReceiptPreprocessingConfig {
        return ReceiptPreprocessingConfig(
            preprocessingLevel = PreprocessingLevel.BASIC,
            qualityPreference = QualityPreference.SPEED,
            captureConfig = CaptureConfig(
                enableHdr = false,
                enableBurstCapture = false
            ),
            imageEnhancementConfig = ImageEnhancementConfig(
                enableNoiseReduction = false,
                enableSharpening = false,
                enableMorphologicalOperations = false
            ),
            layoutAnalysisConfig = LayoutAnalysisConfig(
                enableColumnDetection = false,
                enableTableStructureRecognition = false,
                enableStructuralAnalysis = false
            )
        )
    }
    
    /**
     * Konfigurasi untuk kualitas maksimum (lebih lambat)
     */
    fun qualityOptimizedConfig(): ReceiptPreprocessingConfig {
        return ReceiptPreprocessingConfig(
            preprocessingLevel = PreprocessingLevel.MAXIMUM,
            qualityPreference = QualityPreference.QUALITY,
            captureConfig = CaptureConfig(
                enableHdr = true,
                enableBurstCapture = true,
                burstCount = 5
            ),
            imageEnhancementConfig = ImageEnhancementConfig(
                noiseReductionMethod = NoiseReductionMethod.NON_LOCAL_MEANS,
                thresholdingMethod = ThresholdingMethod.MULTI_LEVEL,
                sharpeningIntensity = 0.7f
            ),
            layoutAnalysisConfig = LayoutAnalysisConfig(
                enableLineSegmentation = true,
                enableSkewCorrection = true,
                enableColumnDetection = true,
                enableTableStructureRecognition = true,
                enableTextOrientation = true,
                enableStructuralAnalysis = true
            )
        )
    }
    
    /**
     * Konfigurasi untuk perangkat dengan memori rendah
     */
    fun lowMemoryDeviceConfig(): ReceiptPreprocessingConfig {
        return ReceiptPreprocessingConfig(
            preprocessingLevel = PreprocessingLevel.BASIC,
            deviceOptimizationConfig = DeviceOptimizationConfig(
                enableGpuAcceleration = false,
                enableLowMemoryMode = true,
                threadCount = 1
            ),
            imageEnhancementConfig = ImageEnhancementConfig(
                enableNoiseReduction = true,
                noiseReductionMethod = NoiseReductionMethod.GAUSSIAN,
                enableSharpening = false,
                enableMorphologicalOperations = false
            )
        )
    }
}