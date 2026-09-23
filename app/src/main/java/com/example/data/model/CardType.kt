package com.example.data.model

enum class CardType(val displayName: String) {
    CREDIT("Crédito"),
    DEBIT("Débito"),
    MULTIPLE("Múltiplo (Crédito e Débito)");

    val supportsCredit: Boolean
        get() = this == CREDIT || this == MULTIPLE

    val supportsDebit: Boolean
        get() = this == DEBIT || this == MULTIPLE

    companion object {
        fun fromString(value: String): CardType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: CREDIT
        }
    }
}
