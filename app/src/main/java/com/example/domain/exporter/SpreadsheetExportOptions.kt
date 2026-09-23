package com.example.domain.exporter

import com.example.data.model.TransactionType

enum class ExportPeriodScope(val displayName: String) {
    ALL("Todos os períodos"),
    CURRENT_MONTH("Mês atual"),
    LAST_THREE_MONTHS("Últimos 3 meses"),
    CURRENT_YEAR("Ano atual")
}

enum class ExportTypeScope(val displayName: String) {
    ALL("Receitas e Despesas"),
    EXPENSE_ONLY("Apenas Despesas"),
    INCOME_ONLY("Apenas Receitas")
}

enum class ExportTransactionScope(val displayName: String) {
    ALL("Todas as movimentações"),
    FILTERED("Resultados dos filtros atuais")
}

data class SpreadsheetExportOptions(
    val transactionScope: ExportTransactionScope = ExportTransactionScope.ALL,
    val periodScope: ExportPeriodScope = ExportPeriodScope.ALL,
    val typeScope: ExportTypeScope = ExportTypeScope.ALL,
    val includeCardsAndInvoices: Boolean = true,
    val customStartMillis: Long? = null,
    val customEndMillis: Long? = null
)
