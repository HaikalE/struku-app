package com.example.struku.domain.model

/**
 * Model class representing an expense category
 */
data class Category(
    val id: String,
    val name: String,
    val color: Long,
    val iconName: String,
    val visible: Boolean
) {
    companion object {
        val DEFAULT_CATEGORIES = listOf(
            Category("food", "Makanan & Minuman", 0xFFE91E63, "restaurant", true),
            Category("groceries", "Belanja Bulanan", 0xFF9C27B0, "shopping_cart", true),
            Category("transportation", "Transportasi", 0xFF3F51B5, "directions_car", true),
            Category("entertainment", "Hiburan", 0xFF03A9F4, "movie", true),
            Category("utilities", "Tagihan & Utilitas", 0xFF009688, "receipt", true),
            Category("health", "Kesehatan", 0xFFFF5722, "medical_services", true),
            Category("education", "Pendidikan", 0xFF4CAF50, "school", true),
            Category("clothing", "Pakaian", 0xFFFFEB3B, "checkroom", true),
            Category("personal", "Perawatan Diri", 0xFFFF9800, "spa", true),
            Category("housing", "Rumah & Sewa", 0xFF795548, "home", true),
            Category("savings", "Tabungan & Investasi", 0xFF607D8B, "savings", true),
            Category("other", "Lainnya", 0xFF9E9E9E, "more_horiz", true)
        )
    }
}