package com.example.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CardType
import com.example.data.model.CreditCardEntity
import com.example.domain.util.CurrencyUtils
import com.example.ui.components.AmountInputField

val cardColorPalette = listOf(
    0xFF8A05BE, // Nubank Purple
    0xFFFF7A00, // Inter Orange
    0xFF0D47A1, // Deep Blue
    0xFFD32F2F, // Santander Red
    0xFF1B5E20, // Emerald Green
    0xFF263238, // Black / Titanium
    0xFFE65100, // Amber / Itaú
    0xFF4A148C  // Violet
)

val cardBrandSuggestions = listOf("Mastercard", "Visa", "Elo", "American Express", "Hipercard")
val bankSuggestions = listOf("Nubank", "Banco Inter", "Itaú", "Bradesco", "Banco do Brasil", "Santander", "C6 Bank", "Caixa")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CardEditSheet(
    card: CreditCardEntity?,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        name: String,
        type: CardType,
        issuer: String,
        network: String,
        colorHex: Long,
        lastFourDigits: String,
        creditLimitCents: Long,
        closingDay: Int,
        dueDay: Int
    ) -> Unit,
    onToggleArchive: ((CreditCardEntity) -> Unit)? = null,
    onDelete: ((CreditCardEntity) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var nameText by remember { mutableStateOf(card?.name ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }

    var selectedType by remember { mutableStateOf(card?.type ?: CardType.CREDIT) }
    var issuerText by remember { mutableStateOf(card?.issuer ?: "") }
    var networkText by remember { mutableStateOf(card?.network ?: "") }
    var selectedColor by remember { mutableLongStateOf(card?.colorHex ?: cardColorPalette.first()) }
    var lastFourDigits by remember { mutableStateOf(card?.lastFourDigits ?: "") }

    var limitAmountText by remember {
        mutableStateOf(
            if (card != null && card.creditLimitCents > 0L) {
                CurrencyUtils.formatCentsToDecimal(card.creditLimitCents)
            } else ""
        )
    }
    var limitError by remember { mutableStateOf<String?>(null) }

    var closingDay by remember { mutableIntStateOf(card?.closingDay ?: 1) }
    var dueDay by remember { mutableIntStateOf(card?.dueDay ?: 10) }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("card_edit_sheet")
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
                    text = if (card == null) stringResource(R.string.new_card) else stringResource(R.string.edit_card),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (card != null) {
                    Row {
                        if (onToggleArchive != null) {
                            IconButton(
                                onClick = {
                                    onToggleArchive(card)
                                    onDismiss()
                                },
                                modifier = Modifier.testTag("archive_card_button")
                            ) {
                                Icon(
                                    imageVector = if (card.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                    contentDescription = if (card.isArchived) "Desarquivar" else "Arquivar",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (onDelete != null) {
                            IconButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.testTag("delete_card_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Aviso de Segurança
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Nunca solicite ou insira número completo, CVV ou senhas. Apenas dados para seu controle financeiro.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tipo de Cartão
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val types = listOf(CardType.CREDIT, CardType.DEBIT, CardType.MULTIPLE)
                types.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                        label = { Text(type.displayName) }
                    )
                }
            }

            // Nome / Apelido
            OutlinedTextField(
                value = nameText,
                onValueChange = {
                    nameText = it
                    nameError = null
                },
                label = { Text("Nome ou Apelido do Cartão") },
                placeholder = { Text("ex.: Nubank Roxinho, Inter Black") },
                isError = nameError != null,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_name_input")
            )
            if (nameError != null) {
                Text(
                    text = nameError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Banco / Emissor
            OutlinedTextField(
                value = issuerText,
                onValueChange = { issuerText = it },
                label = { Text("Banco ou Emissor") },
                placeholder = { Text("ex.: Nubank, Itaú, Inter, Bradesco") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_issuer_input")
            )

            // Bandeira e 4 Últimos Dígitos (lado a lado)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = networkText,
                    onValueChange = { networkText = it },
                    label = { Text("Bandeira (opcional)") },
                    placeholder = { Text("Mastercard, Visa...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.2f)
                )

                OutlinedTextField(
                    value = lastFourDigits,
                    onValueChange = {
                        val filtered = it.filter { ch -> ch.isDigit() }.take(4)
                        lastFourDigits = filtered
                    },
                    label = { Text("Finais (4 dígitos)") },
                    placeholder = { Text("1234") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(0.8f)
                        .testTag("card_digits_input")
                )
            }

            // Limite de Crédito (se suportar crédito)
            if (selectedType.supportsCredit) {
                AmountInputField(
                    value = limitAmountText,
                    onValueChange = {
                        limitAmountText = it
                        limitError = null
                    },
                    label = "Limite de Crédito Total (R$)",
                    isError = limitError != null,
                    errorMessage = limitError
                )

                // Dias de Fechamento e Vencimento
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DayPickerField(
                        label = "Dia Fechamento",
                        selectedDay = closingDay,
                        onDaySelected = { closingDay = it },
                        modifier = Modifier.weight(1f)
                    )

                    DayPickerField(
                        label = "Dia Vencimento",
                        selectedDay = dueDay,
                        onDaySelected = { dueDay = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Paleta de Cores
            Text(
                text = "Aparência do Cartão",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                cardColorPalette.forEach { colorHex ->
                    val isSelected = selectedColor == colorHex
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(colorHex))
                            .clickable { selectedColor = colorHex }
                            .then(
                                if (isSelected) {
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

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
                        if (nameText.trim().isEmpty()) {
                            nameError = "Informe um nome para o cartão."
                            return@Button
                        }
                        var limitCents = 0L
                        if (selectedType.supportsCredit) {
                            val parsed = CurrencyUtils.parseInputToCents(limitAmountText)
                            if (parsed == null || parsed <= 0L) {
                                limitError = "Informe um limite válido maior que zero."
                                return@Button
                            }
                            limitCents = parsed
                        }

                        onSave(
                            card?.id ?: 0L,
                            nameText.trim(),
                            selectedType,
                            if (issuerText.isNotBlank()) issuerText.trim() else "Emissor",
                            networkText.trim(),
                            selectedColor,
                            lastFourDigits.trim(),
                            limitCents,
                            closingDay,
                            dueDay
                        )
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("save_card_button")
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDeleteConfirm && card != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Excluir Cartão") },
            text = { Text("Deseja realmente excluir o cartão '${card.name}'? Se preferir manter o histórico, considere apenas arquivá-lo.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(card)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPickerField(
    label: String,
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = "Dia $selectedDay",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            (1..31).forEach { day ->
                DropdownMenuItem(
                    text = { Text("Dia $day") },
                    onClick = {
                        onDaySelected(day)
                        expanded = false
                    }
                )
            }
        }
    }
}
