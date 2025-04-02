package com.example.struku.domain.repository

import com.example.struku.domain.model.Category
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface untuk operasi terkait kategori
 */
interface CategoryRepository {
    /**
     * Mendapatkan semua kategori (default dan yang ditentukan pengguna)
     */
    fun getAllCategories(): Flow<List<Category>>
    
    /**
     * Menyimpan kategori baru yang ditentukan pengguna
     */
    suspend fun saveCategory(category: Category)
    
    /**
     * Memperbarui kategori yang ada
     */
    suspend fun updateCategory(category: Category)
    
    /**
     * Menghapus kategori yang ditentukan pengguna
     */
    suspend fun deleteCategory(categoryId: String)
    
    /**
     * Mendapatkan kategori berdasarkan ID
     */
    suspend fun getCategoryById(categoryId: String): Category?
    
    /**
     * Memprediksi kategori berdasarkan nama pedagang dan nama item
     */
    suspend fun predictCategory(merchantName: String, items: List<String>): String
}