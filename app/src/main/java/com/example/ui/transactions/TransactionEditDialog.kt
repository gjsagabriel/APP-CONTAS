package com.example.ui.transactions

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CategoryEntity
import com.example.data.model.CreditCardEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.domain.calculator.CreditCardCalculator
import com.example.domain.util.CurrencyUtils
import com.example.domain.util.DateUtils
import com.example.ui.components.AmountInputField
import com.example.ui.components.CategoryIconBadge
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionEditSheet(
    transaction: TransactionEntity?,
    categories: List<CategoryEntity>,
    cards: List<CreditCardEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        description: String,
        amountCents: Long,
        type: TransactionType,
        categoryId: Long,
        categoryName: String,
        dateMillis: Long,
        note: String,
        paymentMethod: PaymentMethod,
        cardId: Long?,
        cardName: String,
        totalInstallments: Int
    ) -> Unit,
    onDelete: ((TransactionEntity, Boolean) -> Unit)? = null,
    onAddNewCard: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var selectedType by remember {
        mutableStateOf(transaction?.type ?: TransactionType.EXPENSE)
    }

    var amountText by remember {
        mutableStateOf(
            if (transaction != null) {
                CurrencyUtils.formatCentsToDecimal(transaction.amountCents)
            } else ""
        )
    }
    var amountError by remember { mutableStateOf<String?>(null) }

    var descriptionText by remember {
        mutableStateOf(transaction?.description ?: "")
    }
    var descriptionError by remember { mutableStateOf<String?>(null) }

    val relevantCategories = remember(categories, selectedType) {
        categories.filter { it.type == selectedType }
    }

    var selectedCategoryId by remember {
        mutableLongStateOf(
            transaction?.categoryId ?: relevantCategories.firstOrNull()?.id ?: 0L
        )
    }

    var dateMillis by remember {
        mutableLongStateOf(transaction?.dateMillis ?: System.currentTimeMillis())
    }

    var noteText by remember {
        mutableStateOf(transaction?.note ?: "")
    }

    // Formas de Pagamento
    var selectedPaymentMethod by remember {
        mutableStateOf(transaction?.paymentMethod ?: PaymentMethod.UNSPECIFIED)
    }

    // Cartão Selecionado
    val eligibleCards = remember(cards, selectedPaymentMethod) {
        val nonArchived = cards.filter { !it.isArchived || it.id == transaction?.cardId }
        when (selectedPaymentMethod) {
            PaymentMethod.CREDIT_CARD -> nonArchived.filter { it.type.supportsCredit }
            PaymentMethod.DEBIT_CARD -> nonArchived.filter { it.type.supportsDebit }
            else -> emptyList()
        }
    }

    var selectedCardId by remember {
        mutableStateOf(
            transaction?.cardId ?: eligibleCards.firstOrNull()?.id
        )
    }
    var cardError by remember { mutableStateOf<String?>(null) }

    // Parcelamento
    var isInstallment by remember {
        mutableStateOf((transaction?.totalInstallments ?: 1) > 1)
    }
    var installmentCount by remember {
        mutableIntStateOf(transaction?.totalInstallments?.coerceAtLeast(2) ?: 2)
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    val selectedCategory = categories.find { it.id == selectedCategoryId }
        ?: relevantCategories.firstOrNull()

    val selectedCard = cards.find { it.id == selectedCardId }

    // Cálculo das parcelas estimadas
    val parsedAmountCents by remember(amountText) {
        derivedStateOf { CurrencyUtils.parseInputToCents(amountText) ?: 0L }
    }

    val installmentPreviews by remember(parsedAmountCents, installmentCount, isInstallment, dateMillis, selectedCard) {
        derivedStateOf {
            if (isInstallment && parsedAmountCents > 0L && selectedCard != null) {
                CreditCardCalculator.generateInstallmentPreviews(
                    totalAmountCents = parsedAmountCents,
                    installmentCount = installmentCount,
                    purchaseDateMillis = dateMillis,
                    closingDay = selectedCard.closingDay
                )
            } else {
                emptyList()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("transaction_edit_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (transaction == null) {
                        stringResource(R.string.new_transaction)
                    } else {
                        stringResource(R.string.edit_transaction)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (transaction != null && onDelete != null) {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("delete_transaction_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Tipo (Receita / Despesa)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val types = listOf(TransactionType.EXPENSE, TransactionType.INCOME)
                types.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = selectedType == type,
                        onClick = {
                            selectedType = type
                            val firstInType = categories.firstOrNull { it.type == type }
                            if (firstInType != null) {
                                selectedCategoryId = firstInType.id
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                        label = { Text(type.displayName) },
                        modifier = Modifier.testTag("type_segmented_${type.name.lowercase()}")
                    )
                }
            }

            // Valor
            AmountInputField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    amountError = null
                },
                label = if (selectedType == TransactionType.EXPENSE && isInstallment) "Valor Total da Compra (R$)" else "Valor (R$)",
                isError = amountError != null,
                errorMessage = amountError
            )

            // Descrição
            OutlinedTextField(
                value = descriptionText,
                onValueChange = {
                    descriptionText = it
                    descriptionError = null
                },
                label = { Text(stringResource(R.string.description)) },
                isError = descriptionError != null,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("description_input_field")
            )
            if (descriptionError != null) {
                Text(
                    text = descriptionError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            // Formas de Pagamento (Exclusivo para Despesas)
            if (selectedType == TransactionType.EXPENSE) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.payment_method),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    val paymentMethods = listOf(
                        PaymentMethod.CREDIT_CARD,
                        PaymentMethod.DEBIT_CARD,
                        PaymentMethod.PIX,
                        PaymentMethod.CASH,
                        PaymentMethod.WALLET
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Se for um item antigo não informado, permite manter até alterar
                        if (selectedPaymentMethod == PaymentMethod.UNSPECIFIED) {
                            FilterChip(
                                selected = true,
                                onClick = {},
                                label = { Text(PaymentMethod.UNSPECIFIED.displayName) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("payment_chip_unspecified")
                            )
                        }

                        paymentMethods.forEach { method ->
                            val isSelected = selectedPaymentMethod == method
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedPaymentMethod = method
                                    cardError = null
                                    // Seleciona o primeiro cartão elegível automaticamente se ainda não tiver selecionado
                                    val suitable = cards.filter { !it.isArchived }.filter {
                                        if (method == PaymentMethod.CREDIT_CARD) it.type.supportsCredit else it.type.supportsDebit
                                    }
                                    if (selectedCardId == null || suitable.none { it.id == selectedCardId }) {
                                        selectedCardId = suitable.firstOrNull()?.id
                                    }
                                },
                                label = { Text(method.displayName) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("payment_chip_${method.name.lowercase()}")
                            )
                        }
                    }
                }

                // Se cartão de crédito ou débito: Selecionar Cartão
                if (selectedPaymentMethod.requiresCard) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Selecionar Cartão",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (eligibleCards.isEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Nenhum cartão cadastrado compatível com ${selectedPaymentMethod.displayName.lowercase()}.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (onAddNewCard != null) {
                                        OutlinedButton(
                                            onClick = onAddNewCard,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("add_card_from_transaction_button")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Cadastrar Novo Cartão")
                                        }
                                    }
                                }
                            }
                        } else {
                            CardDropdownSelector(
                                cards = eligibleCards,
                                selectedCardId = selectedCardId,
                                onCardSelected = {
                                    selectedCardId = it
                                    cardError = null
                                }
                            )
                        }

                        if (cardError != null) {
                            Text(
                                text = cardError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Se Cartão de Crédito e novo lançamento: Opção de Parcelamento
                    if (selectedPaymentMethod == PaymentMethod.CREDIT_CARD && transaction == null) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Modalidade de Pagamento",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )

                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    selected = !isInstallment,
                                    onClick = { isInstallment = false },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    label = { Text("À vista (1x)") }
                                )
                                SegmentedButton(
                                    selected = isInstallment,
                                    onClick = { isInstallment = true },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    label = { Text("Parcelado") }
                                )
                            }

                            // Quantidade de Parcelas e Pré-visualização
                            AnimatedVisibility(visible = isInstallment) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    InstallmentCountSelector(
                                        count = installmentCount,
                                        onCountSelected = { installmentCount = it }
                                    )

                                    if (installmentPreviews.isNotEmpty()) {
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                val firstInst = installmentPreviews.first()
                                                val lastInst = installmentPreviews.last()
                                                Text(
                                                    text = "${installmentCount}x de ${CurrencyUtils.formatCentsToBRL(firstInst.amountCents)}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Text(
                                                    text = "1ª parcela na fatura de ${DateUtils.formatMonthYear(firstInst.invoiceMonthYear)}.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                if (installmentCount > 1) {
                                                    Text(
                                                        text = "Última parcela prevista para a fatura de ${DateUtils.formatMonthYear(lastInst.invoiceMonthYear)}.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
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
            }

            // Categoria
            Text(
                text = stringResource(R.string.category),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                relevantCategories.forEach { category ->
                    val isSelected = category.id == selectedCategoryId
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategoryId = category.id },
                        leadingIcon = {
                            CategoryIconBadge(
                                iconKey = category.iconKey,
                                colorHex = category.colorHex,
                                size = 24.dp,
                                iconSize = 14.dp
                            )
                        },
                        label = { Text(category.name) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("category_chip_${category.id}")
                    )
                }
            }

            // Data
            Card(
                onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val newCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            }
                            dateMillis = newCal.timeInMillis
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("date_picker_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.date),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = DateUtils.formatToBrazilianDate(dateMillis),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = stringResource(R.string.date),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Observações
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Observação (opcional)") },
                maxLines = 2,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_input_field")
            )

            // Botões de Ação
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(stringResource(R.string.cancel))
                }

                Button(
                    onClick = {
                        val parsedCents = CurrencyUtils.parseInputToCents(amountText)
                        var hasError = false

                        if (parsedCents == null || parsedCents <= 0L) {
                            amountError = context.getString(R.string.error_invalid_amount)
                            hasError = true
                        }

                        if (descriptionText.trim().isEmpty()) {
                            descriptionError = context.getString(R.string.error_empty_description)
                            hasError = true
                        }

                        if (selectedCategory == null) {
                            hasError = true
                        }

                        if (selectedType == TransactionType.EXPENSE && selectedPaymentMethod.requiresCard && selectedCardId == null) {
                            cardError = "Selecione um cartão para este pagamento."
                            hasError = true
                        }

                        if (!hasError && parsedCents != null && selectedCategory != null) {
                            val cardObj = cards.find { it.id == selectedCardId }
                            val totalInst = if (selectedType == TransactionType.EXPENSE &&
                                selectedPaymentMethod == PaymentMethod.CREDIT_CARD &&
                                isInstallment
                            ) installmentCount else 1

                            onSave(
                                transaction?.id ?: 0L,
                                descriptionText.trim(),
                                parsedCents,
                                selectedType,
                                selectedCategory.id,
                                selectedCategory.name,
                                dateMillis,
                                noteText.trim(),
                                if (selectedType == TransactionType.EXPENSE) selectedPaymentMethod else PaymentMethod.UNSPECIFIED,
                                if (selectedType == TransactionType.EXPENSE && selectedPaymentMethod.requiresCard) selectedCardId else null,
                                if (selectedType == TransactionType.EXPENSE && selectedPaymentMethod.requiresCard) cardObj?.name ?: "" else "",
                                totalInst
                            )
                            onDismiss()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("save_transaction_button")
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Diálogo de confirmação de exclusão
    if (showDeleteConfirm && transaction != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_transaction)) },
            text = {
                if (transaction.isInstallment) {
                    Text("Esta despesa é parte de uma compra parcelada (${transaction.installmentNumber}/${transaction.totalInstallments}). Deseja excluir apenas esta parcela ou todas as parcelas?")
                } else {
                    Text(stringResource(R.string.delete_confirmation))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(transaction, transaction.isInstallment)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text(if (transaction.isInstallment) "Excluir Todas" else stringResource(R.string.delete))
                }
            },
            dismissButton = {
                if (transaction.isInstallment) {
                    TextButton(
                        onClick = {
                            showDeleteConfirm = false
                            onDelete(transaction, false)
                            onDismiss()
                        }
                    ) {
                        Text("Apenas Esta")
                    }
                } else {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDropdownSelector(
    cards: List<CreditCardEntity>,
    selectedCardId: Long?,
    onCardSelected: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCard = cards.find { it.id == selectedCardId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = if (selectedCard != null) {
                "${selectedCard.name} (${selectedCard.issuer}${if (selectedCard.lastFourDigits.isNotBlank()) " •••• ${selectedCard.lastFourDigits}" else ""})"
            } else "Selecione o cartão",
            onValueChange = {},
            readOnly = true,
            label = { Text("Cartão") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .testTag("card_selector_dropdown")
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            cards.forEach { card ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(card.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "${card.issuer} • ${card.type.displayName}${if (card.lastFourDigits.isNotBlank()) " •••• ${card.lastFourDigits}" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onCardSelected(card.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentCountSelector(
    count: Int,
    onCountSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = "${count}x parcelas",
            onValueChange = {},
            readOnly = true,
            label = { Text("Quantidade de Parcelas") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .testTag("installments_dropdown")
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            (2..36).forEach { num ->
                DropdownMenuItem(
                    text = { Text("${num}x parcelas") },
                    onClick = {
                        onCountSelected(num)
                        expanded = false
                    }
                )
            }
        }
    }
}
