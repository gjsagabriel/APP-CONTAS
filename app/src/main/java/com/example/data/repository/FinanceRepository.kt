package com.example.data.repository

import com.example.data.dao.BudgetDao
import com.example.data.dao.CardInvoicePaymentDao
import com.example.data.dao.CategoryDao
import com.example.data.dao.CreditCardDao
import com.example.data.dao.TransactionDao
import com.example.data.database.PredefinedData
import com.example.data.model.BudgetEntity
import com.example.data.model.CardInvoicePaymentEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.CreditCardEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val creditCardDao: CreditCardDao,
    private val cardInvoicePaymentDao: CardInvoicePaymentDao
) {

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()
    val allCards: Flow<List<CreditCardEntity>> = creditCardDao.getAllCards()
    val activeCards: Flow<List<CreditCardEntity>> = creditCardDao.getActiveCards()
    val allInvoicePayments: Flow<List<CardInvoicePaymentEntity>> = cardInvoicePaymentDao.getAllPayments()

    fun getBudgetsForMonth(monthYear: String): Flow<List<BudgetEntity>> {
        return budgetDao.getBudgetsForMonth(monthYear)
    }

    fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsBetween(startMillis, endMillis)
    }

    fun getTransactionsByCard(cardId: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByCard(cardId)
    }

    fun getPaymentsForCard(cardId: Long): Flow<List<CardInvoicePaymentEntity>> {
        return cardInvoicePaymentDao.getPaymentsForCard(cardId)
    }

    fun getTransactionsByInstallmentGroup(groupId: String): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsByInstallmentGroup(groupId)
    }

    suspend fun ensureDefaultCategories() = withContext(Dispatchers.IO) {
        val count = categoryDao.getCategoryCount()
        if (count == 0) {
            categoryDao.insertCategories(PredefinedData.defaultCategories)
        }
    }

    // Transactions
    suspend fun insertTransaction(transaction: TransactionEntity): Long = withContext(Dispatchers.IO) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long> = withContext(Dispatchers.IO) {
        transactions.map { transactionDao.insertTransaction(it) }
    }

    suspend fun updateTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Long) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun deleteTransactionsByInstallmentGroup(groupId: String) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransactionsByInstallmentGroup(groupId)
    }

    // Categories
    suspend fun insertCategory(category: CategoryEntity): Long = withContext(Dispatchers.IO) {
        categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.deleteCategory(category)
        budgetDao.deleteBudgetsByCategoryId(category.id)
    }

    // Budgets
    suspend fun saveBudget(categoryId: Long, monthYear: String, limitCents: Long): Long = withContext(Dispatchers.IO) {
        val existing = budgetDao.getBudgetForCategoryAndMonth(categoryId, monthYear)
        val budget = if (existing != null) {
            existing.copy(limitCents = limitCents)
        } else {
            BudgetEntity(categoryId = categoryId, monthYear = monthYear, limitCents = limitCents)
        }
        budgetDao.insertOrUpdateBudget(budget)
    }

    suspend fun deleteBudgetById(id: Long) = withContext(Dispatchers.IO) {
        budgetDao.deleteBudgetById(id)
    }

    // Credit Cards
    suspend fun insertCard(card: CreditCardEntity): Long = withContext(Dispatchers.IO) {
        creditCardDao.insertCard(card)
    }

    suspend fun updateCard(card: CreditCardEntity) = withContext(Dispatchers.IO) {
        creditCardDao.updateCard(card)
    }

    suspend fun setCardArchived(cardId: Long, isArchived: Boolean) = withContext(Dispatchers.IO) {
        val card = creditCardDao.getCardByIdSync(cardId)
        if (card != null) {
            creditCardDao.updateCard(card.copy(isArchived = isArchived))
        }
    }

    suspend fun deleteCard(card: CreditCardEntity) = withContext(Dispatchers.IO) {
        creditCardDao.deleteCard(card)
        cardInvoicePaymentDao.deletePaymentsForCard(card.id)
    }

    // Invoice Payments
    suspend fun markInvoiceAsPaid(
        cardId: Long,
        invoiceMonthYear: String,
        amountPaidCents: Long,
        paymentDateMillis: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        val payment = CardInvoicePaymentEntity(
            cardId = cardId,
            invoiceMonthYear = invoiceMonthYear,
            amountPaidCents = amountPaidCents,
            paymentDateMillis = paymentDateMillis,
            isPaid = true
        )
        cardInvoicePaymentDao.insertPayment(payment)
    }

    suspend fun unmarkInvoiceAsPaid(cardId: Long, invoiceMonthYear: String) = withContext(Dispatchers.IO) {
        cardInvoicePaymentDao.deletePayment(cardId, invoiceMonthYear)
    }

    // Demo Data Management
    suspend fun loadDemoData() = withContext(Dispatchers.IO) {
        ensureDefaultCategories()
        val demoCards = PredefinedData.createDemoCards()
        creditCardDao.insertCards(demoCards)
        val demoTxs = PredefinedData.createDemoTransactions()
        val demoBudgets = PredefinedData.createDemoBudgets()
        transactionDao.insertTransactions(demoTxs)
        budgetDao.insertBudgets(demoBudgets)
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        transactionDao.deleteAllTransactions()
        budgetDao.deleteAllBudgets()
        categoryDao.deleteAllCustomCategories()
        creditCardDao.deleteAllCards()
        cardInvoicePaymentDao.deleteAllPayments()
    }
}
