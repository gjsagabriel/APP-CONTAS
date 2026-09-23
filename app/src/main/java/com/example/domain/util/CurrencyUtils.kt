package com.example.domain.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Utilitários para manuseio e exibição precisa de valores monetários.
 * Todos os valores são armazenados internamente em centavos (Long) para evitar
 * problemas de arredondamento de ponto flutuante (sem uso de Double).
 */
object CurrencyUtils {

    private val brazilianLocale: Locale = Locale("pt", "BR")

    /**
     * Formata um valor em centavos para a representação monetária brasileira (ex: R$ 1.250,50).
     */
    fun formatCentsToCurrency(cents: Long): String {
        val format = NumberFormat.getCurrencyInstance(brazilianLocale)
        val bigDecimal = BigDecimal(cents).divide(BigDecimal(100), 2, RoundingMode.HALF_EVEN)
        return format.format(bigDecimal)
    }

    /**
     * Alias para formatação em Real brasileiro (R$).
     */
    fun formatCentsToBRL(cents: Long): String = formatCentsToCurrency(cents)

    /**
     * Formata centavos em formato decimal simples (ex: "1.250,50").
     */
    fun formatCentsToDecimal(cents: Long): String {
        val format = NumberFormat.getNumberInstance(brazilianLocale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val bigDecimal = BigDecimal(cents).divide(BigDecimal(100), 2, RoundingMode.HALF_EVEN)
        return format.format(bigDecimal)
    }

    /**
     * Converte uma string de entrada do usuário (ex: "150,50", "150.50", "1.200,00") para centavos (Long).
     * Retorna null se a string for inválida ou não representar um valor numérico.
     */
    fun parseInputToCents(input: String): Long? {
        val cleaned = input.trim()
            .replace("R$", "")
            .replace(" ", "")
            .replace("\u00A0", "")

        if (cleaned.isEmpty()) return null

        return try {
            // Normalizar separadores: se houver tanto ponto quanto vírgula (ex: 1.250,50)
            val normalized = if (cleaned.contains(",") && cleaned.contains(".")) {
                cleaned.replace(".", "").replace(",", ".")
            } else if (cleaned.contains(",")) {
                cleaned.replace(",", ".")
            } else {
                cleaned
            }

            val bigDecimal = BigDecimal(normalized).setScale(2, RoundingMode.HALF_EVEN)
            val cents = bigDecimal.multiply(BigDecimal(100)).longValueExact()
            cents
        } catch (_: Exception) {
            null
        }
    }
}
