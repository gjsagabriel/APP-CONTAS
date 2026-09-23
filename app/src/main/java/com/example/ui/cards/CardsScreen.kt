package com.example.ui.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CardInvoicePaymentEntity
import com.example.data.model.CreditCardEntity
import com.example.data.model.TransactionEntity
import com.example.domain.model.CardSummary
import com.example.domain.model.InvoiceSummary
import com.example.domain.util.CurrencyUtils
import com.example.domain.util.DateUtils
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(
    viewModel: MainViewModel,
    onAddNewCard: () -> Unit,
    onEditCard: (CreditCardEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardSummaries by viewModel.cardSummaries.collectAsState()
    val selectedCardId by viewModel.selectedCardId.collectAsState()
    val currentSelectedSummary by viewModel.currentSelectedCardSummary.collectAsState()
    val invoices by viewModel.invoicesForSelectedCard.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val allPayments by viewModel.allInvoicePayments.collectAsState()

    var selectedInvoiceTab by remember { mutableIntStateOf(0) } // 0: Atual, 1: Futuras, 2: Histórico
    var inspectingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.cards),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = onAddNewCard,
                        modifier = Modifier.testTag("add_card_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.new_card)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNewCard,
                modifier = Modifier.testTag("fab_add_card")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.new_card)
                )
            }
        },
        modifier = modifier.testTag("cards_screen")
    ) { innerPadding ->
        if (cardSummaries.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Text(
                        text = "Nenhum cartão cadastrado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Cadastre seus cartões de crédito e débito para acompanhar limites, faturas e compras parceladas com precisão.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Button(
                        onClick = onAddNewCard,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("empty_add_card_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cadastrar Cartão")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Horizontal Carousel of Cards
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(cardSummaries) { summary ->
                                val isSelected = currentSelectedSummary?.card?.id == summary.card.id
                                Box(
                                    modifier = Modifier
                                        .width(300.dp)
                                        .clickable { viewModel.setSelectedCardId(summary.card.id) }
                                ) {
                                    VisualCreditCard(
                                        card = summary.card,
                                        modifier = if (isSelected) Modifier else Modifier.clip(RoundedCornerShape(20.dp))
                                    )
                                }
                            }
                        }
                    }
                }

                currentSelectedSummary?.let { summary ->
                    // Card Limit & Cycle Details
                    item {
                        CardLimitDetailsSection(
                            summary = summary,
                            onEdit = { onEditCard(summary.card) }
                        )
                    }

                    // Invoices Section
                    if (summary.card.type.supportsCredit) {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Text(
                                    text = "Faturas do Cartão",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                PrimaryTabRow(
                                    selectedTabIndex = selectedInvoiceTab,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                                ) {
                                    Tab(
                                        selected = selectedInvoiceTab == 0,
                                        onClick = { selectedInvoiceTab = 0 },
                                        text = { Text("Atual", fontWeight = FontWeight.SemiBold) }
                                    )
                                    Tab(
                                        selected = selectedInvoiceTab == 1,
                                        onClick = { selectedInvoiceTab = 1 },
                                        text = { Text("Futuras", fontWeight = FontWeight.SemiBold) }
                                    )
                                    Tab(
                                        selected = selectedInvoiceTab == 2,
                                        onClick = { selectedInvoiceTab = 2 },
                                        text = { Text("Histórico", fontWeight = FontWeight.SemiBold) }
                                    )
                                }
                            }
                        }

                        // Tab Content
                        when (selectedInvoiceTab) {
                            0 -> {
                                // Fatura Atual
                                val currentInvoice = invoices.find { it.monthYear == summary.currentInvoiceMonthYear }
                                    ?: InvoiceSummary(
                                        monthYear = summary.currentInvoiceMonthYear,
                                        cardId = summary.card.id,
                                        closingDateMillis = summary.nextClosingDateMillis,
                                        dueDateMillis = summary.nextDueDateMillis,
                                        totalAmountCents = summary.currentInvoiceAmountCents,
                                        isPaid = summary.isCurrentInvoicePaid,
                                        paymentDateMillis = null,
                                        isClosed = false,
                                        transactions = emptyList()
                                    )

                                item {
                                    InvoiceDetailCard(
                                        invoice = currentInvoice,
                                        isCurrent = true,
                                        onTogglePaid = { isPaid ->
                                            if (isPaid) {
                                                viewModel.markInvoiceAsPaid(
                                                    cardId = summary.card.id,
                                                    invoiceMonthYear = currentInvoice.monthYear,
                                                    amountCents = currentInvoice.totalAmountCents
                                                )
                                            } else {
                                                viewModel.unmarkInvoiceAsPaid(
                                                    cardId = summary.card.id,
                                                    invoiceMonthYear = currentInvoice.monthYear
                                                )
                                            }
                                        },
                                        onTransactionClick = { tx ->
                                            if (tx.isInstallment) {
                                                inspectingTransaction = tx
                                            }
                                        },
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                            1 -> {
                                // Faturas Futuras
                                val futureInvoices = invoices.filter { it.monthYear > summary.currentInvoiceMonthYear }
                                if (futureInvoices.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Nenhuma parcela prevista para faturas futuras.",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                } else {
                                    items(futureInvoices) { inv ->
                                        InvoiceDetailCard(
                                            invoice = inv,
                                            isCurrent = false,
                                            onTogglePaid = { isPaid ->
                                                if (isPaid) {
                                                    viewModel.markInvoiceAsPaid(
                                                        cardId = summary.card.id,
                                                        invoiceMonthYear = inv.monthYear,
                                                        amountCents = inv.totalAmountCents
                                                    )
                                                } else {
                                                    viewModel.unmarkInvoiceAsPaid(
                                                        cardId = summary.card.id,
                                                        invoiceMonthYear = inv.monthYear
                                                    )
                                                }
                                            },
                                            onTransactionClick = { tx ->
                                                if (tx.isInstallment) {
                                                    inspectingTransaction = tx
                                                }
                                            },
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            2 -> {
                                // Histórico (faturas passadas)
                                val pastInvoices = invoices.filter { it.monthYear < summary.currentInvoiceMonthYear }
                                if (pastInvoices.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Nenhuma fatura anterior registrada.",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                } else {
                                    items(pastInvoices) { inv ->
                                        InvoiceDetailCard(
                                            invoice = inv,
                                            isCurrent = false,
                                            onTogglePaid = { isPaid ->
                                                if (isPaid) {
                                                    viewModel.markInvoiceAsPaid(
                                                        cardId = summary.card.id,
                                                        invoiceMonthYear = inv.monthYear,
                                                        amountCents = inv.totalAmountCents
                                                    )
                                                } else {
                                                    viewModel.unmarkInvoiceAsPaid(
                                                        cardId = summary.card.id,
                                                        invoiceMonthYear = inv.monthYear
                                                    )
                                                }
                                            },
                                            onTransactionClick = { tx ->
                                                if (tx.isInstallment) {
                                                    inspectingTransaction = tx
                                                }
                                            },
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal de detalhes da compra parcelada
    inspectingTransaction?.let { tx ->
        val groupTransactions = allTransactions.filter {
            it.installmentGroupId.isNotBlank() && it.installmentGroupId == tx.installmentGroupId
        }
        InstallmentDetailsDialog(
            transaction = tx,
            allInstallments = groupTransactions,
            payments = allPayments,
            onDismiss = { inspectingTransaction = null },
            onDeleteSingle = { transaction ->
                viewModel.deleteTransaction(transaction, deleteAllInstallments = false)
            },
            onDeleteAll = { groupId ->
                viewModel.deleteTransaction(tx, deleteAllInstallments = true)
            }
        )
    }
}

@Composable
fun CardLimitDetailsSection(
    summary: CardSummary,
    onEdit: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Controle de Limites",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_card_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar Cartão",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (summary.card.type.supportsCredit) {
                // Alerta de Limite Excedido
                if (summary.isLimitExceeded) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Limite de Crédito Excedido!",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = "Compras não quitadas ultrapassaram o limite em ${CurrencyUtils.formatCentsToBRL(summary.usedLimitCents - summary.totalLimitCents)}.",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Barra de progresso do limite
                val progress = if (summary.totalLimitCents > 0L) {
                    (summary.usedLimitCents.toFloat() / summary.totalLimitCents.toFloat()).coerceIn(0f, 1f)
                } else 0f

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (summary.isLimitExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${(progress * 100).toInt()}% utilizado",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Total: ${CurrencyUtils.formatCentsToBRL(summary.totalLimitCents)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Grid de valores: Utilizado e Disponível
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Utilizado
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Limite Utilizado",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CurrencyUtils.formatCentsToBRL(summary.usedLimitCents),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.isLimitExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Disponível
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Limite Disponível",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (summary.availableLimitCents < 0L) "R$ 0,00" else CurrencyUtils.formatCentsToBRL(summary.availableLimitCents),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.availableLimitCents > 0L) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Datas do ciclo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Fecha dia ${summary.card.closingDay}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFFFFA000),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Vence dia ${summary.card.dueDay}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            } else {
                Text(
                    text = "Cartão de débito: compras debitam diretamente do saldo da conta, sem faturas nem limite de crédito.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun InvoiceDetailCard(
    invoice: InvoiceSummary,
    isCurrent: Boolean,
    onTogglePaid: (Boolean) -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedPurchases by remember { mutableStateOf(isCurrent) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (invoice.isPaid) {
                Color(0xFFE8F5E9)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Month & Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Fatura ${DateUtils.formatMonthYear(invoice.monthYear)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (invoice.isPaid) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Vencimento: ${DateUtils.formatDate(invoice.dueDateMillis)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                invoice.isPaid -> Color(0xFF4CAF50)
                                invoice.isClosed -> Color(0xFFFFA000)
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when {
                            invoice.isPaid -> "PAGA"
                            invoice.isClosed -> "FECHADA"
                            else -> "ABERTA"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Total Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Valor da Fatura",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyUtils.formatCentsToBRL(invoice.totalAmountCents),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (invoice.isPaid) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Pay / Unpay Button
                if (invoice.isPaid) {
                    OutlinedButton(
                        onClick = { onTogglePaid(false) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("unpay_invoice_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paga", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { onTogglePaid(true) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("pay_invoice_button")
                    ) {
                        Text(stringResource(R.string.mark_as_paid), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Lançamentos incluídos nesta fatura
            if (invoice.transactions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { expandedPurchases = !expandedPurchases }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lançamentos (${invoice.transactions.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (expandedPurchases) "Ocultar" else "Ver detalhes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                AnimatedVisibility(visible = expandedPurchases) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        invoice.transactions.forEach { tx ->
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTransactionClick(tx) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tx.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${DateUtils.formatDate(tx.dateMillis)} • ${tx.categoryName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = CurrencyUtils.formatCentsToBRL(tx.amountCents),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (tx.isInstallment) {
                                            Text(
                                                text = "${tx.installmentNumber}/${tx.totalInstallments}x (Toque para ver)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Nenhum lançamento registrado nesta fatura.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
