package com.example.struku.data.ocr

/**
 * Factory for creating preprocessing configurations
 */
object ReceiptPreprocessingConfigFactory {
    
    /**
     * Create a configuration with specified parameters
     */
    fun createConfig(
        receiptType: ReceiptType = ReceiptType.AUTO_DETECT,
        preprocessingLevel: PreprocessingLevel = PreprocessingLevel.ADVANCED,
        enabled: Boolean = true
    ): ReceiptPreprocessingConfig {
        return ReceiptPreprocessingConfig(
            receiptType = receiptType,
            preprocessingLevel = preprocessingLevel,
            enabled = enabled
        )
    }
    
    /**
     * Create a speed-optimized configuration
     * Uses minimal preprocessing for faster processing
     */
    fun createSpeedOptimizedConfig(): ReceiptPreprocessingConfig {
        return ReceiptPreprocessingConfig(
            receiptType = ReceiptType.AUTO_DETECT,
            preprocessingLevel = PreprocessingLevel.BASIC,
            enabled = true
        )
    }
    
    /**
     * Create a quality-optimized configuration
     * Uses maximum preprocessing for best OCR results
     */
    fun createQualityOptimizedConfig(): ReceiptPreprocessingConfig {
        return ReceiptPreprocessingConfig(
            receiptType = ReceiptType.AUTO_DETECT,
            preprocessingLevel = PreprocessingLevel.MAXIMUM,
            enabled = true
        )
    }
    
    /**
     * Create a balanced configuration (default)
     * Uses advanced preprocessing with auto-detection
     */
    fun createBalancedConfig(): ReceiptPreprocessingConfig {
        return ReceiptPreprocessingConfig(
            receiptType = ReceiptType.AUTO_DETECT,
            preprocessingLevel = PreprocessingLevel.ADVANCED,
            enabled = true
        )
    }
    
    /**
     * Create a configuration optimized for thermal receipts
     */
    fun createThermalReceiptConfig(): ReceiptPreprocessingConfig {
        return ReceiptPreprocessingConfig(
            receiptType = ReceiptType.THERMAL,
            preprocessingLevel = PreprocessingLevel.ADVANCED,
            enabled = true
        )
    }
    
    /**
     * Create a configuration optimized for inkjet/printed receipts
     */
    fun createInkjetReceiptConfig(): ReceiptPreprocessingConfig {
        return ReceiptPreprocessingConfig(
            receiptType = ReceiptType.INKJET,
            preprocessingLevel = PreprocessingLevel.ADVANCED,
            enabled = true
        )
    }
    
    /**
     * Create a configuration optimized for laser-printed receipts
     */
    fun createLaserReceiptConfig(): ReceiptPreprocessingConfig {
        return ReceiptPreprocessingConfig(
            receiptType = ReceiptType.LASER,
            preprocessingLevel = PreprocessingLevel.ADVANCED,
            enabled = true
        )
    }
    
    /**
     * Create a configuration optimized for digital receipts
     */
    fun createDigitalReceiptConfig(): ReceiptPreprocessingConfig {
        return ReceiptPreprocessingConfig(
            receiptType = ReceiptType.DIGITAL,
            preprocessingLevel = PreprocessingLevel.ADVANCED,
            enabled = true
        )
    }
}
