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
     * Advanced preprocessing - balanced approach
     * Includes adaptive thresholding, noise reduction, and basic perspective correction
     */
    ADVANCED,
    
    /**
     * Maximum preprocessing - slower but more accurate
     * Includes all preprocessing steps including advanced perspective correction,
     * multiple thresholding methods, and content-aware enhancements
     */
    MAXIMUM
}
