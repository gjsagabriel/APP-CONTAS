package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val description: String,
    val amountCents: Long,
    val type: TransactionType,
    val categoryId: Long,
    val categoryName: String,
    val dateMillis: Long,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val paymentMethod: PaymentMethod = PaymentMethod.UNSPECIFIED,
    val cardId: Long? = null,
    val cardName: String = "",
    val installmentNumber: Int = 1,
    val totalInstallments: Int = 1,
    val installmentGroupId: String = "",
    val invoiceMonthYear: String = ""
) {
    val isInstallment: Boolean
        get() = totalInstallments > 1
}
