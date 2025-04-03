package com.example.struku.data.ocr

/**
 * Levels of image preprocessing for OCR
 */
enum class PreprocessingLevel {
    /**
     * Basic preprocessing - faster but less accurate
     * Includes only essential steps like grayscale conversion and basic thresholding
     */
    BASIC,
    
    /**
     * Standard preprocessing - balanced approach for most cases
     * Includes adaptive thresholding, noise reduction, and basic perspective correction
     */
    STANDARD,
    
    /**
     * Enhanced preprocessing - more processing for challenging receipts
     * Includes additional steps for handling low contrast and poor lighting
     */
    ENHANCED,
    
    /**
     * Advanced preprocessing - balanced approach with better quality
     * Includes adaptive thresholding, noise reduction, and basic perspective correction
     */
    ADVANCED,
    
    /**
     * Intensive preprocessing - slower but more accurate for difficult cases
     * Includes multiple processing passes and specialized enhancement for receipts
     */
    INTENSIVE,
    
    /**
     * Maximum preprocessing - slower but more accurate
     * Includes all preprocessing steps including advanced perspective correction,
     * multiple thresholding methods, and content-aware enhancements
     */
    MAXIMUM
}