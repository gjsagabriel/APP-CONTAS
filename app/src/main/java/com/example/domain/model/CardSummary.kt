package com.example.domain.model

import com.example.data.model.CreditCardEntity

data class CardSummary(
    val card: CreditCardEntity,
    val totalLimitCents: Long,
    val usedLimitCents: Long,
    val availableLimitCents: Long,
    val isLimitExceeded: Boolean,
    val currentInvoiceMonthYear: String,
    val currentInvoiceAmountCents: Long,
    val isCurrentInvoicePaid: Boolean,
    val nextClosingDateMillis: Long,
    val nextDueDateMillis: Long
)
