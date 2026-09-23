package com.example.ui.budgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionType
import com.example.domain.model.BudgetProgress
import com.example.domain.util.CurrencyUtils
import com.example.domain.util.DateUtils
import com.example.ui.components.AmountInputField
import com.example.ui.components.CategoryIconBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetEditSheet(
    budgetProgress: BudgetProgress?,
    categories: List<CategoryEntity>,
    monthYear: String,
    onDismiss: () -> Unit,
    onSave: (categoryId: Long, limitCents: Long, monthYear: String) -> Unit,
    onDelete: ((Long) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val expenseCategories = remember(categories) {
        categories.filter { it.type == TransactionType.EXPENSE }
    }

    var selectedCategoryId by remember {
        mutableLongStateOf(
            budgetProgress?.categoryId ?: expenseCategories.firstOrNull()?.id ?: 0L
        )
    }

    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    var limitAmountText by remember {
        mutableStateOf(
            if (budgetProgress != null) {
                CurrencyUtils.formatCentsToDecimal(budgetProgress.limitCents)
            } else ""
        )
    }
    var limitError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val selectedCategory = categories.find { it.id == selectedCategoryId }
        ?: expenseCategories.firstOrNull()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("budget_edit_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (budgetProgress == null) "Definir Orçamento" else "Editar Orçamento",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Mês: ${DateUtils.formatMonthYearDisplayName(monthYear)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (budgetProgress != null && onDelete != null) {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.testTag("delete_budget_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Seleção de Categoria
            if (budgetProgress == null) {
                ExposedDropdownMenuBox(
                    expanded = isCategoryDropdownExpanded,
                    onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "Selecione uma categoria",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria de Despesa") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) },
                        leadingIcon = {
                            if (selectedCategory != null) {
                                CategoryIconBadge(
                                    iconKey = selectedCategory.iconKey,
                                    colorHex = selectedCategory.colorHex,
                                    size = 28.dp,
                                    iconSize = 16.dp
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryDropdownExpanded,
                        onDismissRequest = { isCategoryDropdownExpanded = false }
                    ) {
                        expenseCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                leadingIcon = {
                                    CategoryIconBadge(
                                        iconKey = cat.iconKey,
                                        colorHex = cat.colorHex,
                                        size = 24.dp,
                                        iconSize = 14.dp
                                    )
                                },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    isCategoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedCategory != null) {
                        CategoryIconBadge(
                            iconKey = selectedCategory.iconKey,
                            colorHex = selectedCategory.colorHex,
                            size = 36.dp,
                            iconSize = 20.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = selectedCategory.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Limite de Gastos (R$)
            AmountInputField(
                value = limitAmountText,
                onValueChange = {
                    limitAmountText = it
                    limitError = null
                },
                label = "Meta / Limite Máximo (R$)",
                isError = limitError != null,
                errorMessage = limitError
            )

            // Ações
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
                        val parsed = CurrencyUtils.parseInputToCents(limitAmountText)
                        if (parsed == null || parsed <= 0L) {
                            limitError = "Informe um limite válido maior que zero."
                            return@Button
                        }
                        if (selectedCategory == null) return@Button

                        onSave(selectedCategory.id, parsed, monthYear)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("save_budget_button")
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDeleteConfirm && budgetProgress != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Excluir Orçamento") },
            text = { Text("Deseja remover o orçamento para ${budgetProgress.categoryName}?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(budgetProgress.categoryId)
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
