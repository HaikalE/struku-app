package com.example.struku.presentation

/**
 * Navigation routes for the app
 */
object NavRoutes {
    const val RECEIPTS = "receipts"
    const val RECEIPT_DETAIL = "receipt_detail/{receiptId}"
    const val RECEIPT_SCAN = "receipt_scan"
    const val RECEIPT_EDIT = "receipt_edit/{receiptId}"
    const val RECEIPT_ADD = "receipt_add"
    
    const val ANALYTICS = "analytics"
    const val MONTHLY_REPORT = "monthly_report/{year}/{month}"
    const val CATEGORY_REPORT = "category_report/{categoryId}"
    
    const val SETTINGS = "settings"
    const val SETTINGS_EXPORT = "settings_export"
    const val SETTINGS_CATEGORIES = "settings_categories"
    const val SETTINGS_BUDGETS = "settings_budgets"
    
    // Route with arguments
    fun receiptDetail(receiptId: Int) = "receipt_detail/$receiptId"
    fun receiptEdit(receiptId: Int) = "receipt_edit/$receiptId"
    fun monthlyReport(year: Int, month: Int) = "monthly_report/$year/$month"
    fun categoryReport(categoryId: String) = "category_report/$categoryId"
}