package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: CardType = CardType.CREDIT,
    val issuer: String,
    val network: String = "",
    val colorHex: Long = 0xFF8A05BE,
    val lastFourDigits: String = "",
    val creditLimitCents: Long = 0L,
    val closingDay: Int = 1,
    val dueDay: Int = 10,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
