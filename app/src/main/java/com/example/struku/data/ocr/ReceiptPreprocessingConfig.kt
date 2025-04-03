package com.example.struku.data.ocr

/**
 * Configuration for receipt image preprocessing
 */
data class ReceiptPreprocessingConfig(
    /**
     * Type of receipt to optimize for
     */
    val receiptType: ReceiptType = ReceiptType.AUTO_DETECT,
    
    /**
     * Level of preprocessing to apply
     */
    val preprocessingLevel: PreprocessingLevel = PreprocessingLevel.ADVANCED,
    
    /**
     * Enable/disable preprocessing
     */
    val enabled: Boolean = true,
    
    /**
     * Preprocessing parameters
     */
    val params: Map<String, Any> = emptyMap()
)

/**
 * Types of receipts
 */
enum class ReceiptType {
    /**
     * Auto-detect receipt type
     */
    AUTO_DETECT,
    
    /**
     * Thermal printer receipts (typically on thermal paper)
     */
    THERMAL,
    
    /**
     * Inkjet printer receipts
     */
    INKJET,
    
    /**
     * Laser printer receipts
     */
    LASER,
    
    /**
     * Digital receipts (e.g. PDF, e-receipt)
     */
    DIGITAL
}
