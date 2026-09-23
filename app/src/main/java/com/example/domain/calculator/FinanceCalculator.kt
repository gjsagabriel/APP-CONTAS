package com.example.domain.calculator

import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.domain.model.BudgetProgress
import com.example.domain.model.CategorySummary
import com.example.domain.model.DateFilter
import com.example.domain.model.MonthlyFinanceSummary
import com.example.domain.util.DateUtils
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar

object FinanceCalculator {

    fun calculateTotalIncome(transactions: List<TransactionEntity>): Long {
        return transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amountCents }
    }

    fun calculateTotalExpense(transactions: List<TransactionEntity>): Long {
        return transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amountCents }
    }

    fun calculateBalance(transactions: List<TransactionEntity>): Long {
        val income = calculateTotalIncome(transactions)
        val expense = calculateTotalExpense(transactions)
        return income - expense
    }

    fun calculateMonthlySummary(
        monthYear: String,
        transactions: List<TransactionEntity>
    ): MonthlyFinanceSummary {
        val range = DateUtils.getMonthRangeMillis(monthYear)
        val monthTransactions = transactions.filter {
            it.dateMillis in range.first..range.second
        }
        val income = calculateTotalIncome(monthTransactions)
        val expense = calculateTotalExpense(monthTransactions)
        return MonthlyFinanceSummary(
            monthYear = monthYear,
            totalIncomeCents = income,
            totalExpenseCents = expense,
            balanceCents = income - expense
        )
    }

    fun calculateCategorySummaries(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        type: TransactionType = TransactionType.EXPENSE
    ): List<CategorySummary> {
        val filtered = transactions.filter { it.type == type }
        val totalTypeCents = filtered.sumOf { it.amountCents }
        if (totalTypeCents <= 0L) return emptyList()

        val categoryMap = categories.associateBy { it.id }
        val grouped = filtered.groupBy { it.categoryId }

        return grouped.map { (catId, txs) ->
            val cat = categoryMap[catId]
            val totalCatCents = txs.sumOf { it.amountCents }
            val percentage = BigDecimal(totalCatCents)
                .divide(BigDecimal(totalTypeCents), 4, RoundingMode.HALF_EVEN)
                .toFloat()

            CategorySummary(
                categoryId = catId,
                categoryName = cat?.name ?: txs.firstOrNull()?.categoryName ?: "Sem categoria",
                categoryIcon = cat?.iconKey ?: "category",
                categoryColor = cat?.colorHex ?: 0xFF9E9E9E,
                totalCents = totalCatCents,
                percentageOfTotal = percentage,
                count = txs.size
            )
        }.sortedByDescending { it.totalCents }
    }

    fun calculateBudgetProgress(
        budgets: List<BudgetEntity>,
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        monthYear: String
    ): List<BudgetProgress> {
        val range = DateUtils.getMonthRangeMillis(monthYear)
        val monthExpenses = transactions.filter {
            it.type == TransactionType.EXPENSE && it.dateMillis in range.first..range.second
        }

        val expensesByCategory = monthExpenses
            .groupBy { it.categoryId }
            .mapValues { (_, txs) -> txs.sumOf { it.amountCents } }

        val categoryMap = categories.associateBy { it.id }

        return budgets.map { budget ->
            val cat = categoryMap[budget.categoryId]
            val spentCents = expensesByCategory[budget.categoryId] ?: 0L
            val limitCents = budget.limitCents
            val remainingCents = limitCents - spentCents
            val isExceeded = spentCents > limitCents

            val percentage = if (limitCents > 0L) {
                spentCents.toFloat() / limitCents.toFloat()
            } else {
                0.0f
            }

            BudgetProgress(
                categoryId = budget.categoryId,
                categoryName = cat?.name ?: "Categoria #${budget.categoryId}",
                categoryIcon = cat?.iconKey ?: "category",
                categoryColor = cat?.colorHex ?: 0xFF9E9E9E,
                monthYear = budget.monthYear,
                limitCents = limitCents,
                spentCents = spentCents,
                remainingCents = remainingCents,
                percentage = percentage,
                isExceeded = isExceeded
            )
        }.sortedWith(
            compareByDescending<BudgetProgress> { it.isExceeded }
                .thenByDescending { it.percentage }
        )
    }

    fun filterTransactions(
        transactions: List<TransactionEntity>,
        period: DateFilter,
        categoryId: Long?,
        type: TransactionType?,
        searchQuery: String = ""
    ): List<TransactionEntity> {
        val range = getDateRangeForFilter(period)

        return transactions.filter { tx ->
            val matchesPeriod = if (range != null) {
                tx.dateMillis in range.first..range.second
            } else {
                true
            }

            val matchesCategory = categoryId == null || tx.categoryId == categoryId
            val matchesType = type == null || tx.type == type
            val matchesQuery = if (searchQuery.isBlank()) {
                true
            } else {
                tx.description.contains(searchQuery, ignoreCase = true) ||
                        tx.categoryName.contains(searchQuery, ignoreCase = true) ||
                        tx.note.contains(searchQuery, ignoreCase = true)
            }

            matchesPeriod && matchesCategory && matchesType && matchesQuery
        }
    }

    fun getDateRangeForFilter(period: DateFilter): Pair<Long, Long>? {
        val now = Calendar.getInstance()
        return when (period) {
            DateFilter.ALL -> null
            DateFilter.THIS_MONTH -> {
                val cal = Calendar.getInstance()
                val currentMonth = DateUtils.formatToMonthYear(cal)
                DateUtils.getMonthRangeMillis(currentMonth)
            }
            DateFilter.LAST_MONTH -> {
                val cal = Calendar.getInstance()
                val lastMonth = DateUtils.getOffsetMonth(DateUtils.formatToMonthYear(cal), -1)
                DateUtils.getMonthRangeMillis(lastMonth)
            }
            DateFilter.THIS_YEAR -> {
                val startCal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val endCal = Calendar.getInstance().apply {
                    set(Calendar.MONTH, Calendar.DECEMBER)
                    set(Calendar.DAY_OF_MONTH, 31)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                Pair(startCal.timeInMillis, endCal.timeInMillis)
            }
        }
    }
}
