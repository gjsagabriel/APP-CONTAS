package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.util.CurrencyUtils
import com.example.ui.theme.ExpenseRedDark
import com.example.ui.theme.ExpenseRedLight
import com.example.ui.theme.IncomeGreenDark
import com.example.ui.theme.IncomeGreenLight

fun getCategoryIcon(key: String): ImageVector {
    return when (key.lowercase()) {
        "restaurant", "food", "refeição" -> Icons.Default.Restaurant
        "home", "casa", "moradia" -> Icons.Default.Home
        "directions_car", "transporte", "car" -> Icons.Default.DirectionsCar
        "medical_services", "saude", "saúde" -> Icons.Default.MedicalServices
        "school", "educacao", "educação" -> Icons.Default.School
        "movie", "lazer", "entretenimento" -> Icons.Default.Movie
        "shopping_cart", "compras", "cart" -> Icons.Default.ShoppingCart
        "payments", "salario", "salário" -> Icons.Default.Payments
        "trending_up", "rendimentos", "investimento" -> Icons.Default.TrendingUp
        "work", "freelance", "trabalho" -> Icons.Default.Work
        "redeem", "presente", "gift" -> Icons.Default.Redeem
        "account_balance_wallet", "carteira", "outras" -> Icons.Default.AccountBalanceWallet
        "more_horiz", "outros" -> Icons.Default.MoreHoriz
        else -> Icons.Default.Category
    }
}

val availableCategoryIcons = listOf(
    "restaurant",
    "home",
    "directions_car",
    "medical_services",
    "school",
    "movie",
    "shopping_cart",
    "payments",
    "trending_up",
    "work",
    "redeem",
    "account_balance_wallet",
    "category",
    "more_horiz"
)

val availableCategoryColors = listOf(
    0xFFFF7043, // Laranja
    0xFF42A5F5, // Azul
    0xFFFFA726, // Âmbar
    0xFFEF5350, // Vermelho
    0xFFAB47BC, // Roxo
    0xFF26A69A, // Teal
    0xFFEC407A, // Rosa
    0xFF2E7D32, // Verde escuro
    0xFF00897B, // Ciano escuro
    0xFF039BE5, // Azul claro
    0xFF8E24AA, // Púrpura
    0xFF78909C, // Cinza ardósia
    0xFF5C6BC0, // Índigo
    0xFF8D6E63  // Marrom
)

@Composable
fun CategoryIconBadge(
    iconKey: String,
    colorHex: Long,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp
) {
    val bgColor = Color(colorHex).copy(alpha = 0.18f)
    val iconColor = Color(colorHex)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = getCategoryIcon(iconKey),
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun CurrencyText(
    cents: Long,
    modifier: Modifier = Modifier,
    hideValues: Boolean = false,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
    showPrefixSign: Boolean = false
) {
    val isDark = isSystemInDarkTheme()
    val incomeColor = if (isDark) IncomeGreenDark else IncomeGreenLight
    val expenseColor = if (isDark) ExpenseRedDark else ExpenseRedLight

    val textColor = when {
        showPrefixSign && cents > 0 -> incomeColor
        showPrefixSign && cents < 0 -> expenseColor
        else -> color
    }

    val displayText = when {
        hideValues -> "R$ •••••"
        showPrefixSign && cents > 0 -> "+ ${CurrencyUtils.formatCentsToCurrency(cents)}"
        showPrefixSign && cents < 0 -> "- ${CurrencyUtils.formatCentsToCurrency(-cents)}"
        else -> CurrencyUtils.formatCentsToCurrency(cents)
    }

    Text(
        text = displayText,
        style = style,
        color = textColor,
        modifier = modifier
    )
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onActionClick,
                modifier = Modifier.testTag("empty_state_action_button")
            ) {
                Text(actionText)
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (actionText != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun AmountInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Valor (R$)",
    isError: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            prefix = { Text("R$ ") },
            isError = isError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("amount_input_field")
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}
