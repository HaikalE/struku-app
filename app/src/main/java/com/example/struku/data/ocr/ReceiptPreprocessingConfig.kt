package com.example.struku.data.ocr

/**
 * Configuration options for preprocessing receipt images before OCR
 */
data class ReceiptPreprocessingConfig(
    /**
     * Contrast adjustment factor (1.0 is no change)
     */
    val contrastAdjustment: Float = 1.0f,
    
    /**
     * Brightness adjustment value (-255 to 255, 0 is no change)
     */
    val brightnessAdjustment: Float = 0f,
    
    /**
     * Radius for Gaussian blur (0 to disable)
     */
    val gaussianBlurRadius: Float = 0f,
    
    /**
     * Block size for adaptive thresholding (must be odd)
     */
    val adaptiveThresholdBlockSize: Int = 11,
    
    /**
     * Constant value subtracted from the threshold (higher is more aggressive)
     */
    val adaptiveThresholdConstant: Double = 2.0,
    
    /**
     * Maximum dimension to resize the image to (0 to disable)
     */
    val resizeMaxDimension: Int = 0,
    
    /**
     * Whether to perform deskewing to straighten the image
     */
    val performDeskew: Boolean = false,
    
    /**
     * Whether to sharpen the image
     */
    val performSharpen: Boolean = false,
    
    /**
     * Amount of sharpening to apply (higher is more aggressive)
     */
    val sharpenAmount: Float = 1.0f,
    
    /**
     * Whether to convert the image to grayscale
     */
    val convertToGrayscale: Boolean = true,
    
    /**
     * Whether to detect and crop just the receipt from the image
     */
    val detectAndCropReceipt: Boolean = false,
    
    /**
     * Whether to perform histogram normalization
     */
    val performNormalization: Boolean = false,
    
    /**
     * Whether to apply receipt-specific optimizations
     */
    val optimizeForReceipts: Boolean = true,
    
    /**
     * Level of preprocessing to apply
     */
    val processingLevel: PreprocessingLevel = PreprocessingLevel.STANDARD,
    
    /**
     * Whether to optimize specifically for Western receipt formats with clear columns
     */
    val optimizeForWesternReceipts: Boolean = false,
    
    /**
     * Enhanced detection for currency symbols (especially dollar signs)
     */
    val enhancedCurrencyDetection: Boolean = false,
    
    /**
     * Enable noise reduction techniques for better OCR
     */
    val applyNoiseReduction: Boolean = true,
    
    /**
     * Enable detection of column structure in receipts
     */
    val enableColumnDetection: Boolean = false,
    
    /**
     * Detection of grid/table lines in the receipt
     */
    val detectTableStructure: Boolean = false,
    
    /**
     * Enable removal of background patterns often found in receipts
     */
    val removeBackgroundPatterns: Boolean = true
)