package com.example.struku.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val color: Int,
    val iconName: String,
    val isDefault: Boolean,
    val isUserDefined: Boolean
)
