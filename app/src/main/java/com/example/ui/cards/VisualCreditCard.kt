package com.example.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CreditCardEntity

@Composable
fun VisualCreditCard(
    card: CreditCardEntity,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val baseColor = Color(card.colorHex)
    val lighterColor = baseColor.copy(alpha = 0.85f)
    val darkerColor = Color(
        red = (baseColor.red * 0.7f).coerceIn(0f, 1f),
        green = (baseColor.green * 0.7f).coerceIn(0f, 1f),
        blue = (baseColor.blue * 0.7f).coerceIn(0f, 1f)
    )

    val gradient = Brush.linearGradient(
        colors = listOf(lighterColor, baseColor, darkerColor)
    )

    Card(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.6f)
            .testTag("visual_card_${card.id}")
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.matchParentSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Issuer & Archived Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = card.issuer.uppercase(),
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (card.isArchived) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.35f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Archive,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Arquivado",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                // Middle: Chip & Contactless icons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 38.dp, height = 28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE2B755)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "Chip",
                            tint = Color(0xFF634A1B),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Contactless,
                        contentDescription = "Contactless",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Card Number representation
                val digits = if (card.lastFourDigits.isNotBlank()) card.lastFourDigits else "••••"
                Text(
                    text = "••••  ••••  ••••  $digits",
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )

                // Bottom: Card Name and Network
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = card.type.displayName.uppercase(),
                            color = Color.White.copy(alpha = 0.65f),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = card.name,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (card.network.isNotBlank()) {
                        Text(
                            text = card.network,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        }
    }
}
