package com.example.domain.model

import com.example.data.model.TransactionEntity

data class InvoiceSummary(
    val monthYear: String,
    val cardId: Long,
    val closingDateMillis: Long,
    val dueDateMillis: Long,
    val totalAmountCents: Long,
    val isPaid: Boolean,
    val paymentDateMillis: Long?,
    val isClosed: Boolean,
    val transactions: List<TransactionEntity>
)
