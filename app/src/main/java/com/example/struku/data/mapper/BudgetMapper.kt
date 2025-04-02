package com.example.struku.data.mapper

import com.example.struku.data.local.entity.BudgetEntity
import com.example.struku.domain.model.Budget
import java.util.Date

/**
 * Maps domain model to entity
 */
fun Budget.toBudgetEntity(): BudgetEntity {
    return BudgetEntity(
        id = id,
        categoryId = categoryId,
        amount = amount,
        month = month,
        year = year,
        createdAt = createdAt
    )
}

/**
 * Maps entity to domain model
 */
fun BudgetEntity.toBudget(): Budget {
    return Budget(
        id = id,
        categoryId = categoryId,
        amount = amount,
        month = month,
        year = year,
        createdAt = createdAt
    )
}
