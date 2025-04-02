package com.example.struku.presentation

/**
 * Navigation routes for the app
 */
object NavRoutes {
    // Main tabs
    const val RECEIPTS = "receipts"
    const val ANALYTICS = "analytics"
    const val SETTINGS = "settings"
    
    // Receipt related
    const val RECEIPT_DETAIL = "receipt_detail/{receiptId}"
    const val RECEIPT_EDIT = "receipt_edit/{receiptId}"
    const val RECEIPT_ADD = "receipt_add"
    
    // Scanner related
    const val SCANNER = "scanner"
    const val RECEIPT_SCAN = "receipt_scan"  // Added missing route
    const val RECEIPT_REVIEW = "receipt_review/{receiptId}"
    
    // Analytics related
    const val MONTHLY_REPORT = "monthly_report/{year}/{month}"
    const val CATEGORY_REPORT = "category_report/{categoryId}"
    
    // Settings related
    const val SETTINGS_EXPORT = "settings_export"
    const val SETTINGS_CATEGORIES = "settings_categories"
    const val SETTINGS_BUDGETS = "settings_budgets"
    
    // Route helpers with arguments
    fun receiptDetail(receiptId: Int) = "receipt_detail/$receiptId"
    fun receiptEdit(receiptId: Int) = "receipt_edit/$receiptId"
    fun receiptReview(receiptId: Int) = "receipt_review/$receiptId"
    fun monthlyReport(year: Int, month: Int) = "monthly_report/$year/$month"
    fun categoryReport(categoryId: String) = "category_report/$categoryId"
}