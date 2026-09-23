package com.example.ui.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.CategoryEntity
import com.example.data.model.CreditCardEntity
import com.example.data.model.TransactionEntity
import com.example.domain.model.BudgetProgress
import com.example.ui.MainViewModel
import com.example.ui.budgets.BudgetEditSheet
import com.example.ui.budgets.BudgetsScreen
import com.example.ui.cards.CardEditSheet
import com.example.ui.cards.CardsScreen
import com.example.ui.categories.CategoriesScreen
import com.example.ui.categories.CategoryEditSheet
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.transactions.TransactionEditSheet
import com.example.ui.transactions.TransactionsScreen

enum class ScreenTab(
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD(
        R.string.nav_dashboard,
        Icons.Filled.Dashboard,
        Icons.Outlined.Dashboard,
        "nav_dashboard_tab"
    ),
    TRANSACTIONS(
        R.string.nav_transactions,
        Icons.Filled.ReceiptLong,
        Icons.Outlined.ReceiptLong,
        "nav_transactions_tab"
    ),
    CARDS(
        R.string.nav_cards,
        Icons.Filled.CreditCard,
        Icons.Outlined.CreditCard,
        "nav_cards_tab"
    ),
    BUDGETS(
        R.string.nav_budgets,
        Icons.Filled.PieChart,
        Icons.Outlined.PieChart,
        "nav_budgets_tab"
    ),
    CATEGORIES(
        R.string.nav_categories,
        Icons.Filled.Category,
        Icons.Outlined.Category,
        "nav_categories_tab"
    ),
    SETTINGS(
        R.string.nav_settings,
        Icons.Filled.Settings,
        Icons.Outlined.Settings,
        "nav_settings_tab"
    )
}

@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(ScreenTab.DASHBOARD) }

    // Dialog & Sheet States
    var isTransactionSheetOpen by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    var isCardSheetOpen by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<CreditCardEntity?>(null) }

    var isCategorySheetOpen by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    var isBudgetSheetOpen by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetProgress?>(null) }

    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val allCards by viewModel.allCards.collectAsStateWithLifecycle()
    val selectedMonthYear by viewModel.selectedMonthYear.collectAsStateWithLifecycle()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        if (isWideScreen) {
            // Tablet / Foldable NavigationRail layout
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight()
                ) {
                    ScreenTab.values().forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = stringResource(tab.titleRes)
                                )
                            },
                            label = { Text(stringResource(tab.titleRes)) },
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }

                ScreenContent(
                    tab = currentTab,
                    viewModel = viewModel,
                    onNavigateToTransactions = { currentTab = ScreenTab.TRANSACTIONS },
                    onNavigateToBudgets = { currentTab = ScreenTab.BUDGETS },
                    onNavigateToCards = { currentTab = ScreenTab.CARDS },
                    onOpenTransactionDialog = { tx ->
                        editingTransaction = tx
                        isTransactionSheetOpen = true
                    },
                    onOpenCardDialog = { card ->
                        editingCard = card
                        isCardSheetOpen = true
                    },
                    onOpenCategoryDialog = { cat ->
                        editingCategory = cat
                        isCategorySheetOpen = true
                    },
                    onOpenBudgetDialog = { bg ->
                        editingBudget = bg
                        isBudgetSheetOpen = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        } else {
            // Mobile NavigationBar layout
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bottom_navigation_bar")
                    ) {
                        ScreenTab.values().forEach { tab ->
                            val isSelected = currentTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.titleRes)
                                    )
                                },
                                label = { Text(stringResource(tab.titleRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                ScreenContent(
                    tab = currentTab,
                    viewModel = viewModel,
                    onNavigateToTransactions = { currentTab = ScreenTab.TRANSACTIONS },
                    onNavigateToBudgets = { currentTab = ScreenTab.BUDGETS },
                    onNavigateToCards = { currentTab = ScreenTab.CARDS },
                    onOpenTransactionDialog = { tx ->
                        editingTransaction = tx
                        isTransactionSheetOpen = true
                    },
                    onOpenCardDialog = { card ->
                        editingCard = card
                        isCardSheetOpen = true
                    },
                    onOpenCategoryDialog = { cat ->
                        editingCategory = cat
                        isCategorySheetOpen = true
                    },
                    onOpenBudgetDialog = { bg ->
                        editingBudget = bg
                        isBudgetSheetOpen = true
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }

    // Modal Sheet de Lançamento
    if (isTransactionSheetOpen) {
        TransactionEditSheet(
            transaction = editingTransaction,
            categories = allCategories,
            cards = allCards,
            onDismiss = {
                isTransactionSheetOpen = false
                editingTransaction = null
            },
            onSave = { id, desc, amount, type, catId, catName, date, note, paymentMethod, cardId, cardName, totalInstallments ->
                viewModel.saveTransaction(
                    id = id,
                    description = desc,
                    amountCents = amount,
                    type = type,
                    categoryId = catId,
                    categoryName = catName,
                    dateMillis = date,
                    note = note,
                    paymentMethod = paymentMethod,
                    cardId = cardId,
                    cardName = cardName,
                    totalInstallments = totalInstallments
                )
            },
            onDelete = { tx, deleteAll ->
                viewModel.deleteTransaction(tx, deleteAllInstallments = deleteAll)
            },
            onAddNewCard = {
                editingCard = null
                isCardSheetOpen = true
            }
        )
    }

    // Modal Sheet de Cartão
    if (isCardSheetOpen) {
        CardEditSheet(
            card = editingCard,
            onDismiss = {
                isCardSheetOpen = false
                editingCard = null
            },
            onSave = { id, name, type, issuer, network, colorHex, lastDigits, limitCents, closingDay, dueDay ->
                viewModel.saveCard(
                    id = id,
                    name = name,
                    type = type,
                    issuer = issuer,
                    network = network,
                    colorHex = colorHex,
                    lastFourDigits = lastDigits,
                    creditLimitCents = limitCents,
                    closingDay = closingDay,
                    dueDay = dueDay
                )
            },
            onToggleArchive = { card ->
                viewModel.toggleArchiveCard(card)
            },
            onDelete = { card ->
                viewModel.deleteCard(card)
            }
        )
    }

    // Modal Sheet de Categoria
    if (isCategorySheetOpen) {
        CategoryEditSheet(
            category = editingCategory,
            onDismiss = {
                isCategorySheetOpen = false
                editingCategory = null
            },
            onSave = { id, name, iconKey, colorHex, type ->
                viewModel.saveCategory(
                    id = id,
                    name = name,
                    iconKey = iconKey,
                    colorHex = colorHex,
                    type = type
                )
            },
            onDelete = { cat ->
                viewModel.deleteCategory(cat)
            }
        )
    }

    // Modal Sheet de Orçamento
    if (isBudgetSheetOpen) {
        BudgetEditSheet(
            budgetProgress = editingBudget,
            categories = allCategories,
            monthYear = selectedMonthYear,
            onDismiss = {
                isBudgetSheetOpen = false
                editingBudget = null
            },
            onSave = { categoryId, limitCents, monthYear ->
                viewModel.saveBudget(categoryId, limitCents, monthYear)
            },
            onDelete = { categoryId ->
                val budgetEntity = viewModel.allBudgets.value.find {
                    it.categoryId == categoryId && it.monthYear == selectedMonthYear
                }
                if (budgetEntity != null) {
                    viewModel.deleteBudget(budgetEntity.id)
                }
            }
        )
    }
}

@Composable
fun ScreenContent(
    tab: ScreenTab,
    viewModel: MainViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToCards: () -> Unit,
    onOpenTransactionDialog: (TransactionEntity?) -> Unit,
    onOpenCardDialog: (CreditCardEntity?) -> Unit,
    onOpenCategoryDialog: (CategoryEntity?) -> Unit,
    onOpenBudgetDialog: (BudgetProgress?) -> Unit,
    modifier: Modifier = Modifier
) {
    when (tab) {
        ScreenTab.DASHBOARD -> DashboardScreen(
            viewModel = viewModel,
            onNavigateToTransactions = onNavigateToTransactions,
            onNavigateToBudgets = onNavigateToBudgets,
            onNavigateToCards = onNavigateToCards,
            onOpenTransactionDialog = onOpenTransactionDialog,
            modifier = modifier
        )
        ScreenTab.TRANSACTIONS -> TransactionsScreen(
            viewModel = viewModel,
            onOpenTransactionDialog = onOpenTransactionDialog,
            modifier = modifier
        )
        ScreenTab.CARDS -> CardsScreen(
            viewModel = viewModel,
            onAddNewCard = { onOpenCardDialog(null) },
            onEditCard = { card -> onOpenCardDialog(card) },
            modifier = modifier
        )
        ScreenTab.BUDGETS -> BudgetsScreen(
            viewModel = viewModel,
            onOpenBudgetDialog = onOpenBudgetDialog,
            modifier = modifier
        )
        ScreenTab.CATEGORIES -> CategoriesScreen(
            viewModel = viewModel,
            onOpenCategoryDialog = onOpenCategoryDialog,
            modifier = modifier
        )
        ScreenTab.SETTINGS -> SettingsScreen(
            viewModel = viewModel,
            modifier = modifier
        )
    }
}
