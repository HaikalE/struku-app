package com.example.struku.data.ocr

/**
 * Factory for creating preprocessing configurations for receipt images
 */
object ReceiptPreprocessingConfigFactory {
    /**
     * Create a default configuration for receipt preprocessing
     */
    fun createConfig(): ReceiptPreprocessingConfig {
        return ReceiptPreprocessingConfig(
            // Increase contrast to make text more visible
            contrastAdjustment = 1.5f,
            
            // Apply gaussian blur to reduce noise
            gaussianBlurRadius = 0.5f,
            
            // Apply adaptive thresholding to better isolate text
            // Enhanced to better handle dark text on light background
            adaptiveThresholdBlockSize = 31,
            adaptiveThresholdConstant = 11.0,
            
            // Resize image to optimal size for OCR
            resizeMaxDimension = 2000,
            
            // Enhanced deskewing - better handle rotated receipts
            performDeskew = true,
            
            // Sharpen image to make text clearer
            performSharpen = true,
            sharpenAmount = 1.2f,
            
            // Use grayscale for better results
            convertToGrayscale = true,
            
            // Detect and crop receipt from background
            detectAndCropReceipt = true,
            
            // Image normalization and additional settings
            performNormalization = true,
            
            // Apply specific optimizations for receipts
            // Improved whitespace handling and edge detection
            optimizeForReceipts = true,
            
            // Processing level - higher level means more processing
            // Use ENHANCED for better results with struk format
            preprocessingLevel = PreprocessingLevel.ENHANCED
        )
    }
    
    /**
     * Create a basic configuration with minimal processing
     */
    fun createBasicConfig(): ReceiptPreprocessingConfig {
        return ReceiptPreprocessingConfig(
            contrastAdjustment = 1.2f,
            gaussianBlurRadius = 0.0f,
            adaptiveThresholdBlockSize = 21,
            adaptiveThresholdConstant = 10.0,
            resizeMaxDimension = 1600,
            performDeskew = false,
            performSharpen = false,
            sharpenAmount = 0.0f,
            convertToGrayscale = true,
            detectAndCropReceipt = false,
            performNormalization = false,
            optimizeForReceipts = true,
            preprocessingLevel = PreprocessingLevel.BASIC
        )
    }
    
    /**
     * Create an advanced configuration with moderate processing
     */
    fun createAdvancedConfig(): ReceiptPreprocessingConfig {
        return ReceiptPreprocessingConfig(
            contrastAdjustment = 1.5f,
            gaussianBlurRadius = 0.5f,
            adaptiveThresholdBlockSize = 25,
            adaptiveThresholdConstant = 12.0,
            resizeMaxDimension = 1800,
            performDeskew = true,
            performSharpen = true,
            sharpenAmount = 1.0f,
            convertToGrayscale = true,
            detectAndCropReceipt = true,
            performNormalization = true,
            optimizeForReceipts = true,
            preprocessingLevel = PreprocessingLevel.ADVANCED
        )
    }
    
    /**
     * Create a maximum configuration with intensive processing
     */
    fun createMaxConfig(): ReceiptPreprocessingConfig {
        return ReceiptPreprocessingConfig(
            contrastAdjustment = 1.8f,
            brightnessAdjustment = 10f,
            gaussianBlurRadius = 0.8f,
            adaptiveThresholdBlockSize = 35,
            adaptiveThresholdConstant = 15.0,
            resizeMaxDimension = 2000,
            performDeskew = true,
            performSharpen = true,
            sharpenAmount = 1.5f,
            convertToGrayscale = true,
            detectAndCropReceipt = true,
            performNormalization = true,
            optimizeForReceipts = true,
            preprocessingLevel = PreprocessingLevel.MAXIMUM
        )
    }
    
    /**
     * Create a configuration optimized for low-light or blurry images
     */
    fun createLowLightConfig(): ReceiptPreprocessingConfig {
        return ReceiptPreprocessingConfig(
            contrastAdjustment = 1.8f,
            brightnessAdjustment = 20f,
            gaussianBlurRadius = 0.8f,
            adaptiveThresholdBlockSize = 35,
            adaptiveThresholdConstant = 15.0,
            resizeMaxDimension = 2000,
            performDeskew = true,
            performSharpen = true,
            sharpenAmount = 1.5f,
            convertToGrayscale = true,
            detectAndCropReceipt = true,
            performNormalization = true,
            optimizeForReceipts = true,
            preprocessingLevel = PreprocessingLevel.INTENSIVE
        )
    }
    
    /**
     * Create a configuration optimized specifically for US style receipts
     * with standard column-based formats
     */
    fun createWesternReceiptConfig(): ReceiptPreprocessingConfig {
        return ReceiptPreprocessingConfig(
            contrastAdjustment = 1.4f,
            brightnessAdjustment = 10f,
            gaussianBlurRadius = 0.5f,
            adaptiveThresholdBlockSize = 25,
            adaptiveThresholdConstant = 10.0,
            resizeMaxDimension = 2000,
            performDeskew = true,
            performSharpen = true,
            sharpenAmount = 1.2f,
            convertToGrayscale = true,
            detectAndCropReceipt = true,
            performNormalization = true,
            optimizeForReceipts = true,
            // Uses specific optimizations for columnar receipts
            preprocessingLevel = PreprocessingLevel.ENHANCED,
            optimizeForWesternReceipts = true
        )
    }
    
    /**
     * Create a specific configuration for dealing with dollar signs and western formats
     * in receipts 
     */
    fun createDollarFormatConfig(): ReceiptPreprocessingConfig {
        return createWesternReceiptConfig().copy(
            // Enhanced processing for currencies and symbols
            enhancedCurrencyDetection = true,
            // Apply additional contrast to make currency symbols clear
            contrastAdjustment = 1.6f
        )
    }
}