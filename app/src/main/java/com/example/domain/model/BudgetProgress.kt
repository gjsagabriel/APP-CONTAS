package com.example.domain.model

import com.example.data.model.CategoryEntity

data class BudgetProgress(
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Long,
    val monthYear: String,
    val limitCents: Long,
    val spentCents: Long,
    val remainingCents: Long,
    val percentage: Float, // 0.0f to 1.0f or > 1.0f if exceeded
    val isExceeded: Boolean
)

data class CategorySummary(
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: Long,
    val totalCents: Long,
    val percentageOfTotal: Float,
    val count: Int
)

data class MonthlyFinanceSummary(
    val monthYear: String,
    val totalIncomeCents: Long,
    val totalExpenseCents: Long,
    val balanceCents: Long
)
