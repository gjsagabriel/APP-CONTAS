package com.example.data.model

enum class PaymentMethod(
    val displayName: String,
    val requiresCard: Boolean
) {
    UNSPECIFIED("Não informado", false),
    CREDIT_CARD("Cartão de crédito", true),
    DEBIT_CARD("Cartão de débito", true),
    CASH("Dinheiro", false),
    PIX("Pix", false),
    WALLET("Carteira", false);

    companion object {
        fun fromString(value: String): PaymentMethod {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: UNSPECIFIED
        }
    }
}
