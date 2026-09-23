package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.model.BudgetEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FinanceRepositoryRoomTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FinanceRepository(
            transactionDao = database.transactionDao(),
            categoryDao = database.categoryDao(),
            budgetDao = database.budgetDao(),
            creditCardDao = database.creditCardDao(),
            cardInvoicePaymentDao = database.cardInvoicePaymentDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndQueryTransaction() = runBlocking {
        val tx = TransactionEntity(
            description = "Café",
            amountCents = 850L,
            type = TransactionType.EXPENSE,
            categoryId = 1L,
            categoryName = "Alimentação",
            dateMillis = System.currentTimeMillis()
        )
        val id = repository.insertTransaction(tx)
        assertTrue(id > 0)

        val all = repository.allTransactions.first()
        assertEquals(1, all.size)
        assertEquals("Café", all.first().description)
        assertEquals(850L, all.first().amountCents)
    }

    @Test
    fun updateTransaction() = runBlocking {
        val tx = TransactionEntity(
            description = "Almoço",
            amountCents = 3000L,
            type = TransactionType.EXPENSE,
            categoryId = 1L,
            categoryName = "Alimentação",
            dateMillis = 1000L
        )
        val id = repository.insertTransaction(tx)

        val inserted = repository.allTransactions.first().first()
        val updated = inserted.copy(amountCents = 3500L, description = "Almoço Executivo")
        repository.updateTransaction(updated)

        val result = repository.allTransactions.first().first()
        assertEquals(3500L, result.amountCents)
        assertEquals("Almoço Executivo", result.description)
    }

    @Test
    fun deleteTransaction() = runBlocking {
        val tx = TransactionEntity(
            description = "Uber",
            amountCents = 2500L,
            type = TransactionType.EXPENSE,
            categoryId = 2L,
            categoryName = "Transporte",
            dateMillis = 1000L
        )
        val id = repository.insertTransaction(tx)
        assertEquals(1, repository.allTransactions.first().size)

        repository.deleteTransactionById(id)
        assertEquals(0, repository.allTransactions.first().size)
    }

    @Test
    fun insertAndQueryCustomCategory() = runBlocking {
        val cat = CategoryEntity(
            name = "Investimentos",
            iconKey = "trending_up",
            colorHex = 0xFF00897B,
            type = TransactionType.INCOME,
            isPredefined = false
        )
        val id = repository.insertCategory(cat)
        assertTrue(id > 0)

        val categories = repository.allCategories.first()
        assertTrue(categories.any { it.name == "Investimentos" })
    }

    @Test
    fun saveAndQueryBudget() = runBlocking {
        repository.saveBudget(categoryId = 5L, monthYear = "2026-09", limitCents = 50000L)

        val budgets = repository.getBudgetsForMonth("2026-09").first()
        assertEquals(1, budgets.size)
        assertEquals(50000L, budgets.first().limitCents)
    }
}
