package com.example.struku.data.repository

import com.example.struku.data.local.dao.CategoryDao
import com.example.struku.data.mapper.toCategory
import com.example.struku.data.mapper.toCategoryEntity
import com.example.struku.domain.model.Category
import com.example.struku.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { entities ->
            entities.map { it.toCategory() }
        }
    }

    override suspend fun saveCategory(category: Category) {
        categoryDao.insertCategory(category.toCategoryEntity())
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category.toCategoryEntity())
    }

    override suspend fun deleteCategory(categoryId: String) {
        val category = categoryDao.getCategoryById(categoryId) ?: return
        categoryDao.deleteCategory(category)
    }

    override suspend fun getCategoryById(categoryId: String): Category? {
        return categoryDao.getCategoryById(categoryId)?.toCategory()
    }

    override suspend fun predictCategory(merchantName: String, items: List<String>): String {
        // Simple keyword-based categorization
        val keywords = mapOf(
            "groceries" to listOf("market", "supermarket", "mart", "grocery", "food", "buah", "sayur", "indomaret", "alfamart", "carrefour"),
            "dining" to listOf("restaurant", "cafe", "coffee", "restoran", "food court", "makan", "burger", "pizza", "mcd", "kfc"),
            "transport" to listOf("gas", "fuel", "pertamina", "shell", "transport", "toll", "taxi", "uber", "grab", "gojek", "train", "bus"),
            "shopping" to listOf("mall", "store", "shop", "boutique", "retail", "fashion", "clothing", "pakaian", "sepatu", "shoes"),
            "utilities" to listOf("electric", "water", "utility", "bill", "phone", "internet", "pln", "pdam", "telkom", "wifi", "pulsa"),
            "health" to listOf("doctor", "hospital", "pharmacy", "clinic", "drug", "medicine", "apotek", "obat", "rumah sakit", "klinik"),
            "entertainment" to listOf("movie", "cinema", "theater", "concert", "game", "bioskop", "film", "xxi", "cgv")
        )
        
        // Convert merchant name and items to lowercase for case-insensitive matching
        val lowerMerchant = merchantName.lowercase()
        val lowerItems = items.map { it.lowercase() }
        
        // Check merchant name against keywords
        for ((category, words) in keywords) {
            for (word in words) {
                if (lowerMerchant.contains(word)) {
                    return category
                }
            }
        }
        
        // Check items against keywords if merchant match not found
        for ((category, words) in keywords) {
            for (item in lowerItems) {
                for (word in words) {
                    if (item.contains(word)) {
                        return category
                    }
                }
            }
        }
        
        // Default category if no match found
        return "other"
    }
}
