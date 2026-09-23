package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "card_invoice_payments",
    indices = [
        Index(value = ["cardId", "invoiceMonthYear"], unique = true)
    ]
)
data class CardInvoicePaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cardId: Long,
    val invoiceMonthYear: String,
    val amountPaidCents: Long,
    val paymentDateMillis: Long = System.currentTimeMillis(),
    val isPaid: Boolean = true
)
