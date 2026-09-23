package com.example

import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.domain.calculator.FinanceCalculator
import com.example.domain.model.DateFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class FinanceCalculatorTest {

    @Test
    fun calculateIncome_sumsOnlyIncomeTransactions() {
        val transactions = listOf(
            TransactionEntity(id = 1, description = "Salário", amountCents = 500000L, type = TransactionType.INCOME, categoryId = 1, categoryName = "Salário", dateMillis = 1000L),
            TransactionEntity(id = 2, description = "Freelance", amountCents = 150000L, type = TransactionType.INCOME, categoryId = 2, categoryName = "Freelance", dateMillis = 2000L),
            TransactionEntity(id = 3, description = "Mercado", amountCents = 80000L, type = TransactionType.EXPENSE, categoryId = 3, categoryName = "Alimentação", dateMillis = 3000L)
        )

        val totalIncome = FinanceCalculator.calculateTotalIncome(transactions)
        assertEquals(650000L, totalIncome)
    }

    @Test
    fun calculateExpense_sumsOnlyExpenseTransactions() {
        val transactions = listOf(
            TransactionEntity(id = 1, description = "Aluguel", amountCents = 120000L, type = TransactionType.EXPENSE, categoryId = 1, categoryName = "Moradia", dateMillis = 1000L),
            TransactionEntity(id = 2, description = "Luz", amountCents = 25050L, type = TransactionType.EXPENSE, categoryId = 1, categoryName = "Moradia", dateMillis = 2000L),
            TransactionEntity(id = 3, description = "Salário", amountCents = 500000L, type = TransactionType.INCOME, categoryId = 2, categoryName = "Salário", dateMillis = 3000L)
        )

        val totalExpense = FinanceCalculator.calculateTotalExpense(transactions)
        assertEquals(145050L, totalExpense)
    }

    @Test
    fun calculateBalance_subtractsExpensesFromIncomePrecisely() {
        val transactions = listOf(
            TransactionEntity(id = 1, description = "Receita A", amountCents = 100050L, type = TransactionType.INCOME, categoryId = 1, categoryName = "A", dateMillis = 1000L),
            TransactionEntity(id = 2, description = "Despesa B", amountCents = 30025L, type = TransactionType.EXPENSE, categoryId = 2, categoryName = "B", dateMillis = 2000L)
        )

        val balance = FinanceCalculator.calculateBalance(transactions)
        assertEquals(70025L, balance)
    }

    @Test
    fun calculateBudgetProgress_underBudget() {
        val categories = listOf(CategoryEntity(id = 1, name = "Alimentação", type = TransactionType.EXPENSE))
        val budgets = listOf(BudgetEntity(id = 1, categoryId = 1, monthYear = "2026-09", limitCents = 100000L)) // Limite R$ 1.000,00

        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 15, 12, 0, 0)
        }

        val transactions = listOf(
            TransactionEntity(id = 1, description = "Almoço", amountCents = 40000L, type = TransactionType.EXPENSE, categoryId = 1, categoryName = "Alimentação", dateMillis = cal.timeInMillis)
        )

        val progress = FinanceCalculator.calculateBudgetProgress(budgets, categories, transactions, "2026-09")
        assertEquals(1, progress.size)
        val item = progress.first()
        assertEquals(40000L, item.spentCents)
        assertEquals(60000L, item.remainingCents)
        assertEquals(0.4f, item.percentage, 0.001f)
        assertFalse(item.isExceeded)
    }

    @Test
    fun calculateBudgetProgress_exceededBudget() {
        val categories = listOf(CategoryEntity(id = 1, name = "Lazer", type = TransactionType.EXPENSE))
        val budgets = listOf(BudgetEntity(id = 1, categoryId = 1, monthYear = "2026-09", limitCents = 20000L)) // Limite R$ 200,00

        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 10, 12, 0, 0)
        }

        val transactions = listOf(
            TransactionEntity(id = 1, description = "Show", amountCents = 25000L, type = TransactionType.EXPENSE, categoryId = 1, categoryName = "Lazer", dateMillis = cal.timeInMillis)
        )

        val progress = FinanceCalculator.calculateBudgetProgress(budgets, categories, transactions, "2026-09")
        val item = progress.first()
        assertEquals(25000L, item.spentCents)
        assertEquals(-5000L, item.remainingCents)
        assertTrue(item.percentage > 1.0f)
        assertTrue(item.isExceeded)
    }

    @Test
    fun filterTransactions_byTypeAndCategory() {
        val txs = listOf(
            TransactionEntity(id = 1, description = "Mercado", amountCents = 10000L, type = TransactionType.EXPENSE, categoryId = 10, categoryName = "Alimentação", dateMillis = 1000L),
            TransactionEntity(id = 2, description = "Farmácia", amountCents = 5000L, type = TransactionType.EXPENSE, categoryId = 20, categoryName = "Saúde", dateMillis = 2000L),
            TransactionEntity(id = 3, description = "Salário", amountCents = 300000L, type = TransactionType.INCOME, categoryId = 30, categoryName = "Salário", dateMillis = 3000L)
        )

        val expenses = FinanceCalculator.filterTransactions(
            transactions = txs,
            period = DateFilter.ALL,
            categoryId = null,
            type = TransactionType.EXPENSE
        )
        assertEquals(2, expenses.size)

        val onlyHealth = FinanceCalculator.filterTransactions(
            transactions = txs,
            period = DateFilter.ALL,
            categoryId = 20,
            type = null
        )
        assertEquals(1, onlyHealth.size)
        assertEquals("Farmácia", onlyHealth.first().description)
    }
}
