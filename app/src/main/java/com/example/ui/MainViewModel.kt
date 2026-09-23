package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.BudgetEntity
import com.example.data.model.CardInvoicePaymentEntity
import com.example.data.model.CardType
import com.example.data.model.CategoryEntity
import com.example.data.model.CreditCardEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.preferences.ThemeMode
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.FinanceRepository
import com.example.domain.calculator.CreditCardCalculator
import com.example.domain.calculator.FinanceCalculator
import com.example.domain.model.BudgetProgress
import com.example.domain.model.CardSummary
import com.example.domain.model.CategorySummary
import com.example.domain.model.DateFilter
import com.example.domain.model.InvoiceSummary
import com.example.domain.model.MonthlyFinanceSummary
import com.example.domain.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.content.Context
import android.net.Uri
import com.example.domain.exporter.SpreadsheetExportOptions
import com.example.domain.exporter.SpreadsheetExporter
import com.example.ui.export.ExportUiState
import kotlinx.coroutines.Dispatchers
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = FinanceRepository(
        transactionDao = database.transactionDao(),
        categoryDao = database.categoryDao(),
        budgetDao = database.budgetDao(),
        creditCardDao = database.creditCardDao(),
        cardInvoicePaymentDao = database.cardInvoicePaymentDao()
    )
    private val preferencesRepository = UserPreferencesRepository(application)

    val themeMode: StateFlow<ThemeMode> = preferencesRepository.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val hideBalance: StateFlow<Boolean> = preferencesRepository.hideBalanceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBudgets: StateFlow<List<BudgetEntity>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCards: StateFlow<List<CreditCardEntity>> = repository.allCards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCards: StateFlow<List<CreditCardEntity>> = repository.activeCards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInvoicePayments: StateFlow<List<CardInvoicePaymentEntity>> = repository.allInvoicePayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected month for dashboard ("YYYY-MM")
    private val _selectedMonthYear = MutableStateFlow(DateUtils.getCurrentMonthYear())
    val selectedMonthYear: StateFlow<String> = _selectedMonthYear.asStateFlow()

    // Transaction list filters
    private val _selectedPeriodFilter = MutableStateFlow(DateFilter.THIS_MONTH)
    val selectedPeriodFilter: StateFlow<DateFilter> = _selectedPeriodFilter.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<Long?>(null)
    val selectedCategoryFilter: StateFlow<Long?> = _selectedCategoryFilter.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow<TransactionType?>(null)
    val selectedTypeFilter: StateFlow<TransactionType?> = _selectedTypeFilter.asStateFlow()

    private val _selectedPaymentMethodFilter = MutableStateFlow<PaymentMethod?>(null)
    val selectedPaymentMethodFilter: StateFlow<PaymentMethod?> = _selectedPaymentMethodFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filtered transactions for the transactions screen
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        combine(allTransactions, selectedPeriodFilter, selectedCategoryFilter) { txs, period, catId ->
            Triple(txs, period, catId)
        },
        combine(selectedTypeFilter, selectedPaymentMethodFilter, searchQuery) { type, method, query ->
            Triple(type, method, query)
        }
    ) { (txs, period, catId), (type, method, query) ->
        var list = FinanceCalculator.filterTransactions(
            transactions = txs,
            period = period,
            categoryId = catId,
            type = type,
            searchQuery = query
        )
        if (method != null) {
            list = list.filter { it.paymentMethod == method }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Monthly summary for dashboard
    val currentMonthSummary: StateFlow<MonthlyFinanceSummary> = combine(
        allTransactions,
        selectedMonthYear
    ) { txs, monthYear ->
        FinanceCalculator.calculateMonthlySummary(monthYear, txs)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MonthlyFinanceSummary(DateUtils.getCurrentMonthYear(), 0L, 0L, 0L)
    )

    // Category spending distribution for dashboard
    val categoryDistribution: StateFlow<List<CategorySummary>> = combine(
        allTransactions,
        allCategories,
        selectedMonthYear
    ) { txs, cats, monthYear ->
        val range = DateUtils.getMonthRangeMillis(monthYear)
        val monthTxs = txs.filter { it.dateMillis in range.first..range.second }
        FinanceCalculator.calculateCategorySummaries(monthTxs, cats, TransactionType.EXPENSE)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Budget progress for selected month
    val budgetProgressList: StateFlow<List<BudgetProgress>> = combine(
        allBudgets,
        allCategories,
        allTransactions,
        selectedMonthYear
    ) { budgets, cats, txs, monthYear ->
        val monthBudgets = budgets.filter { it.monthYear == monthYear }
        FinanceCalculator.calculateBudgetProgress(monthBudgets, cats, txs, monthYear)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Resumos de todos os cartões de crédito
    val cardSummaries: StateFlow<List<CardSummary>> = combine(
        allCards,
        allTransactions,
        allInvoicePayments
    ) { cards, txs, payments ->
        cards.map { card ->
            CreditCardCalculator.calculateCardSummary(card, txs, payments)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cartão selecionado na tela de cartões
    private val _selectedCardId = MutableStateFlow<Long?>(null)
    val selectedCardId: StateFlow<Long?> = _selectedCardId.asStateFlow()

    // Resumo do cartão selecionado
    val currentSelectedCardSummary: StateFlow<CardSummary?> = combine(
        cardSummaries,
        selectedCardId
    ) { summaries, selectedId ->
        if (selectedId != null) {
            summaries.find { it.card.id == selectedId }
        } else {
            summaries.firstOrNull()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Faturas do cartão selecionado
    val invoicesForSelectedCard: StateFlow<List<InvoiceSummary>> = combine(
        currentSelectedCardSummary,
        allTransactions,
        allInvoicePayments
    ) { cardSummary, txs, payments ->
        if (cardSummary != null) {
            CreditCardCalculator.calculateInvoicesForCard(cardSummary.card, txs, payments)
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.ensureDefaultCategories()
        }
    }

    fun setSelectedMonthYear(monthYear: String) {
        _selectedMonthYear.value = monthYear
    }

    fun nextMonth() {
        _selectedMonthYear.value = DateUtils.getOffsetMonth(_selectedMonthYear.value, 1)
    }

    fun previousMonth() {
        _selectedMonthYear.value = DateUtils.getOffsetMonth(_selectedMonthYear.value, -1)
    }

    fun resetToCurrentMonth() {
        _selectedMonthYear.value = DateUtils.getCurrentMonthYear()
    }

    fun setPeriodFilter(filter: DateFilter) {
        _selectedPeriodFilter.value = filter
    }

    fun setCategoryFilter(categoryId: Long?) {
        _selectedCategoryFilter.value = categoryId
    }

    fun setTypeFilter(type: TransactionType?) {
        _selectedTypeFilter.value = type
    }

    fun setPaymentMethodFilter(method: PaymentMethod?) {
        _selectedPaymentMethodFilter.value = method
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCardId(cardId: Long) {
        _selectedCardId.value = cardId
    }

    fun toggleHideBalance() {
        viewModelScope.launch {
            preferencesRepository.setHideBalance(!hideBalance.value)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun saveTransaction(
        id: Long = 0,
        description: String,
        amountCents: Long,
        type: TransactionType,
        categoryId: Long,
        categoryName: String,
        dateMillis: Long,
        note: String = "",
        paymentMethod: PaymentMethod = PaymentMethod.UNSPECIFIED,
        cardId: Long? = null,
        cardName: String = "",
        totalInstallments: Int = 1
    ) {
        viewModelScope.launch {
            if (type == TransactionType.EXPENSE &&
                paymentMethod == PaymentMethod.CREDIT_CARD &&
                cardId != null &&
                totalInstallments > 1 &&
                id == 0L
            ) {
                // Compra parcelada nova: gerar todas as parcelas determinísticas
                val card = allCards.value.find { it.id == cardId }
                val closingDay = card?.closingDay ?: 1
                val previews = CreditCardCalculator.generateInstallmentPreviews(
                    totalAmountCents = amountCents,
                    installmentCount = totalInstallments,
                    purchaseDateMillis = dateMillis,
                    closingDay = closingDay
                )
                val groupId = UUID.randomUUID().toString()

                val transactionsToInsert = previews.map { preview ->
                    TransactionEntity(
                        description = "$description (${preview.installmentNumber}/$totalInstallments)",
                        amountCents = preview.amountCents,
                        type = TransactionType.EXPENSE,
                        categoryId = categoryId,
                        categoryName = categoryName,
                        dateMillis = preview.dateMillis,
                        note = if (note.isNotBlank()) "$note • Parcela ${preview.installmentNumber}/$totalInstallments" else "Parcela ${preview.installmentNumber}/$totalInstallments",
                        paymentMethod = paymentMethod,
                        cardId = cardId,
                        cardName = cardName,
                        installmentNumber = preview.installmentNumber,
                        totalInstallments = totalInstallments,
                        installmentGroupId = groupId,
                        invoiceMonthYear = preview.invoiceMonthYear
                    )
                }
                repository.insertTransactions(transactionsToInsert)
            } else {
                // Transação comum (à vista, débito, pix, dinheiro, receita ou edição)
                val invoiceMonthYear = if (type == TransactionType.EXPENSE && paymentMethod == PaymentMethod.CREDIT_CARD && cardId != null) {
                    val card = allCards.value.find { it.id == cardId }
                    val closingDay = card?.closingDay ?: 1
                    CreditCardCalculator.getInvoiceCycleForPurchase(dateMillis, closingDay)
                } else {
                    ""
                }

                val tx = TransactionEntity(
                    id = id,
                    description = description,
                    amountCents = amountCents,
                    type = type,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    dateMillis = dateMillis,
                    note = note,
                    paymentMethod = paymentMethod,
                    cardId = cardId,
                    cardName = cardName,
                    installmentNumber = 1,
                    totalInstallments = 1,
                    installmentGroupId = "",
                    invoiceMonthYear = invoiceMonthYear
                )
                if (id == 0L) {
                    repository.insertTransaction(tx)
                } else {
                    repository.updateTransaction(tx)
                }
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity, deleteAllInstallments: Boolean = false) {
        viewModelScope.launch {
            if (deleteAllInstallments && transaction.installmentGroupId.isNotBlank()) {
                repository.deleteTransactionsByInstallmentGroup(transaction.installmentGroupId)
            } else {
                repository.deleteTransaction(transaction)
            }
        }
    }

    fun deleteTransactionById(id: Long) {
        viewModelScope.launch {
            repository.deleteTransactionById(id)
        }
    }

    // Card Actions
    fun saveCard(
        id: Long = 0,
        name: String,
        type: CardType,
        issuer: String,
        network: String = "",
        colorHex: Long = 0xFF8A05BE,
        lastFourDigits: String = "",
        creditLimitCents: Long = 0L,
        closingDay: Int = 1,
        dueDay: Int = 10
    ) {
        viewModelScope.launch {
            val cleanDigits = lastFourDigits.filter { it.isDigit() }.takeLast(4)
            val card = CreditCardEntity(
                id = id,
                name = name,
                type = type,
                issuer = issuer,
                network = network,
                colorHex = colorHex,
                lastFourDigits = cleanDigits,
                creditLimitCents = if (type.supportsCredit) creditLimitCents else 0L,
                closingDay = closingDay.coerceIn(1, 31),
                dueDay = dueDay.coerceIn(1, 31),
                isArchived = false
            )
            if (id == 0L) {
                val newId = repository.insertCard(card)
                _selectedCardId.value = newId
            } else {
                repository.updateCard(card)
            }
        }
    }

    fun toggleArchiveCard(card: CreditCardEntity) {
        viewModelScope.launch {
            repository.setCardArchived(card.id, !card.isArchived)
        }
    }

    fun deleteCard(card: CreditCardEntity) {
        viewModelScope.launch {
            repository.deleteCard(card)
            if (_selectedCardId.value == card.id) {
                _selectedCardId.value = allCards.value.firstOrNull { it.id != card.id }?.id
            }
        }
    }

    // Invoice Payments
    fun markInvoiceAsPaid(cardId: Long, invoiceMonthYear: String, amountCents: Long) {
        viewModelScope.launch {
            repository.markInvoiceAsPaid(cardId, invoiceMonthYear, amountCents)
        }
    }

    fun unmarkInvoiceAsPaid(cardId: Long, invoiceMonthYear: String) {
        viewModelScope.launch {
            repository.unmarkInvoiceAsPaid(cardId, invoiceMonthYear)
        }
    }

    fun saveCategory(
        id: Long = 0,
        name: String,
        iconKey: String,
        colorHex: Long,
        type: TransactionType
    ) {
        viewModelScope.launch {
            val cat = CategoryEntity(
                id = id,
                name = name,
                iconKey = iconKey,
                colorHex = colorHex,
                type = type,
                isPredefined = false
            )
            if (id == 0L) {
                repository.insertCategory(cat)
            } else {
                repository.updateCategory(cat)
            }
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun saveBudget(categoryId: Long, limitCents: Long, monthYear: String = _selectedMonthYear.value) {
        viewModelScope.launch {
            repository.saveBudget(categoryId, monthYear, limitCents)
        }
    }

    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            repository.deleteBudgetById(id)
        }
    }

    fun loadDemoData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.loadDemoData()
            onComplete()
        }
    }

    fun clearAllData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearAllData()
            onComplete()
        }
    }

    // =========================================================================
    // EXPORTAÇÃO DE PLANILHA .XLSX
    // =========================================================================

    private val _exportUiState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val exportUiState: StateFlow<ExportUiState> = _exportUiState.asStateFlow()

    fun exportSpreadsheet(
        context: Context,
        uri: Uri,
        options: SpreadsheetExportOptions,
        fileName: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _exportUiState.value = ExportUiState.Exporting
            try {
                val outputStream = context.contentResolver.openOutputStream(uri)
                    ?: throw IllegalStateException("Não foi possível abrir o arquivo para gravação.")
                outputStream.use { stream ->
                    SpreadsheetExporter.export(
                        outputStream = stream,
                        options = options,
                        allTransactions = allTransactions.value,
                        filteredTransactions = filteredTransactions.value,
                        cards = allCards.value,
                        payments = allInvoicePayments.value
                    )
                }
                _exportUiState.value = ExportUiState.Success(fileName, uri)
            } catch (e: Exception) {
                _exportUiState.value = ExportUiState.Error(
                    e.localizedMessage ?: "Ocorreu um erro ao gerar e salvar a planilha."
                )
            }
        }
    }

    fun dismissExportState() {
        _exportUiState.value = ExportUiState.Idle
    }
}
