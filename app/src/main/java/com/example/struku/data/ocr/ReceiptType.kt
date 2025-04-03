package com.example.struku.data.ocr

/**
 * Types of receipts to optimize preprocessing for
 */
enum class ReceiptType {
    /**
     * Standard thermal paper receipts (common for retail)
     */
    THERMAL,
    
    /**
     * Inkjet printed receipts
     */
    INKJET,
    
    /**
     * Laser printed receipts (typically high quality)
     */
    LASER,
    
    /**
     * Standard for most receipts when type is unknown
     */
    STANDARD
}
