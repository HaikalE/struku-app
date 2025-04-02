package com.example.struku.data.mapper

import com.example.struku.data.local.entity.CategoryEntity
import com.example.struku.domain.model.Category

/**
 * Maps domain model to entity
 */
fun Category.toCategoryEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name,
        color = color,
        iconName = iconName,
        isDefault = isDefault,
        isUserDefined = isUserDefined
    )
}

/**
 * Maps entity to domain model
 */
fun CategoryEntity.toCategory(): Category {
    return Category(
        id = id,
        name = name,
        color = color,
        iconName = iconName,
        isDefault = isDefault,
        isUserDefined = isUserDefined
    )
}
