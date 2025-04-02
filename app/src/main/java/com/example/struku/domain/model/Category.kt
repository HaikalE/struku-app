package com.example.struku.domain.model

/**
 * Model domain untuk kategori pengeluaran
 */
data class Category(
    val id: String,
    val name: String,
    val color: Int,
    val iconName: String,
    val isDefault: Boolean = false,
    val isUserDefined: Boolean = false
) {
    companion object {
        // Kategori default
        val GROCERIES = Category("groceries", "Bahan Makanan", 0xFF4CAF50, "cart", true)
        val DINING = Category("dining", "Makan di Luar", 0xFFFF9800, "restaurant", true)
        val TRANSPORT = Category("transport", "Transportasi", 0xFF2196F3, "car", true)
        val SHOPPING = Category("shopping", "Belanja", 0xFFE91E63, "shopping_bag", true)
        val UTILITIES = Category("utilities", "Tagihan", 0xFF9C27B0, "receipt", true)
        val HEALTH = Category("health", "Kesehatan", 0xFFF44336, "medication", true)
        val ENTERTAINMENT = Category("entertainment", "Hiburan", 0xFFFF5722, "movie", true)
        val OTHER = Category("other", "Lainnya", 0xFF607D8B, "more_horiz", true)
        
        // Daftar semua kategori default
        val DEFAULT_CATEGORIES = listOf(
            GROCERIES, DINING, TRANSPORT, SHOPPING, UTILITIES, HEALTH, ENTERTAINMENT, OTHER
        )
    }
}