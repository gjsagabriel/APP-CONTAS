package com.example.domain.model

data class InstallmentPreview(
    val installmentNumber: Int,
    val totalInstallments: Int,
    val amountCents: Long,
    val dateMillis: Long,
    val invoiceMonthYear: String
)
