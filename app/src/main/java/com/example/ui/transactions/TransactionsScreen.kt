package com.example.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.export.ExportSpreadsheetDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.domain.calculator.FinanceCalculator
import com.example.domain.model.DateFilter
import com.example.domain.util.CurrencyUtils
import com.example.domain.util.DateUtils
import com.example.ui.MainViewModel
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.CurrencyText
import com.example.ui.components.EmptyStateView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: MainViewModel,
    onOpenTransactionDialog: (TransactionEntity?) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriodFilter.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val selectedPaymentMethod by viewModel.selectedPaymentMethodFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val hideBalance by viewModel.hideBalance.collectAsStateWithLifecycle()

    val totalIncome = FinanceCalculator.calculateTotalIncome(filteredTransactions)
    val totalExpense = FinanceCalculator.calculateTotalExpense(filteredTransactions)

    var showExportDialog by remember { mutableStateOf(false) }
    val hasActiveFilters = selectedPeriod != DateFilter.ALL || selectedCategory != null || selectedType != null || selectedPaymentMethod != null || searchQuery.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.nav_transactions),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("btn_export_transactions_top")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Exportar Planilha",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onOpenTransactionDialog(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_transaction_screen")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.new_transaction)
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Campo de Busca
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Buscar por descrição, cartão, nota...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpar busca"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("transactions_search_field")
            )

            // Filtros de Tipo e Período (Horizontal Chips)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Filtro Tipo: Todos
                FilterChip(
                    selected = selectedType == null,
                    onClick = { viewModel.setTypeFilter(null) },
                    label = { Text("Todos") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("filter_type_all")
                )
                // Despesa
                FilterChip(
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = { viewModel.setTypeFilter(TransactionType.EXPENSE) },
                    label = { Text("Despesas") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("filter_type_expense")
                )
                // Receita
                FilterChip(
                    selected = selectedType == TransactionType.INCOME,
                    onClick = { viewModel.setTypeFilter(TransactionType.INCOME) },
                    label = { Text("Receitas") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("filter_type_income")
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // Filtro Período
                DateFilter.values().forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { viewModel.setPeriodFilter(period) },
                        label = { Text(period.label) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("filter_period_${period.name.lowercase()}")
                    )
                }
            }

            // Filtros de Forma de Pagamento (Horizontal Chips)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedPaymentMethod == null,
                    onClick = { viewModel.setPaymentMethodFilter(null) },
                    label = { Text("Todas Formas") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("filter_method_all")
                )

                val methods = listOf(
                    PaymentMethod.CREDIT_CARD,
                    PaymentMethod.DEBIT_CARD,
                    PaymentMethod.PIX,
                    PaymentMethod.CASH,
                    PaymentMethod.WALLET
                )
                methods.forEach { method ->
                    FilterChip(
                        selected = selectedPaymentMethod == method,
                        onClick = { viewModel.setPaymentMethodFilter(method) },
                        label = { Text(method.displayName) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("filter_method_${method.name.lowercase()}")
                    )
                }
            }

            // Filtros de Categoria (Horizontal Chips)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { viewModel.setCategoryFilter(null) },
                    label = { Text("Todas Categorias") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("filter_category_all")
                )

                allCategories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat.id,
                        onClick = { viewModel.setCategoryFilter(cat.id) },
                        label = { Text(cat.name) },
                        leadingIcon = {
                            CategoryIconBadge(
                                iconKey = cat.iconKey,
                                colorHex = cat.colorHex,
                                size = 20.dp,
                                iconSize = 12.dp
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("filter_category_${cat.id}")
                    )
                }
            }

            // Resumo do Filtro (Card compacto)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredTransactions.size} registro(s)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "+ ${if (hideBalance) "••••" else CurrencyUtils.formatCentsToCurrency(totalIncome)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "- ${if (hideBalance) "••••" else CurrencyUtils.formatCentsToCurrency(totalExpense)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Lista de Lançamentos
            if (filteredTransactions.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.FilterList,
                    title = "Nenhum lançamento no filtro",
                    subtitle = "Tente alterar os filtros ou adicione uma nova transação.",
                    actionText = "Limpar Filtros",
                    onActionClick = {
                        viewModel.setPeriodFilter(DateFilter.ALL)
                        viewModel.setTypeFilter(null)
                        viewModel.setPaymentMethodFilter(null)
                        viewModel.setCategoryFilter(null)
                        viewModel.setSearchQuery("")
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        TransactionCard(
                            transaction = tx,
                            hideBalance = hideBalance,
                            onClick = { onOpenTransactionDialog(tx) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        ExportSpreadsheetDialog(
            viewModel = viewModel,
            hasActiveFilters = hasActiveFilters,
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
fun TransactionCard(
    transaction: TransactionEntity,
    hideBalance: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_item_${transaction.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconBadge(
                iconKey = "category",
                colorHex = if (transaction.type == TransactionType.INCOME) 0xFF2E7D32 else 0xFFEF5350,
                size = 40.dp,
                iconSize = 22.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.categoryName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = DateUtils.formatToBrazilianDate(transaction.dateMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Tag da Forma de Pagamento e Cartão
                if (transaction.type == TransactionType.EXPENSE && transaction.paymentMethod != PaymentMethod.UNSPECIFIED) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (transaction.cardName.isNotBlank()) {
                                    "${transaction.paymentMethod.displayName}: ${transaction.cardName}"
                                } else {
                                    transaction.paymentMethod.displayName
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (transaction.isInstallment) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${transaction.installmentNumber}/${transaction.totalInstallments}x",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (transaction.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1
                    )
                }
            }
            CurrencyText(
                cents = transaction.amountCents,
                hideValues = hideBalance,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                showPrefixSign = true
            )
        }
    }
}
