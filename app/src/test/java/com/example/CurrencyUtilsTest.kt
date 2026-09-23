package com.example

import com.example.domain.util.CurrencyUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyUtilsTest {

    @Test
    fun parseInputToCents_withCommaDecimal() {
        val cents = CurrencyUtils.parseInputToCents("150,50")
        assertEquals(15050L, cents)
    }

    @Test
    fun parseInputToCents_withDotDecimal() {
        val cents = CurrencyUtils.parseInputToCents("99.90")
        assertEquals(9990L, cents)
    }

    @Test
    fun parseInputToCents_withThousandsSeparator() {
        val cents = CurrencyUtils.parseInputToCents("1.250,75")
        assertEquals(125075L, cents)
    }

    @Test
    fun parseInputToCents_withCurrencySymbolAndSpaces() {
        val cents = CurrencyUtils.parseInputToCents("R$ 3.400,00")
        assertEquals(340000L, cents)
    }

    @Test
    fun parseInputToCents_wholeNumber() {
        val cents = CurrencyUtils.parseInputToCents("50")
        assertEquals(5000L, cents)
    }

    @Test
    fun parseInputToCents_invalidInputsReturnNull() {
        assertNull(CurrencyUtils.parseInputToCents(""))
        assertNull(CurrencyUtils.parseInputToCents("abc"))
        assertNull(CurrencyUtils.parseInputToCents("R$"))
    }

    @Test
    fun formatCentsToCurrency_containsRealSymbolAndDecimals() {
        val formatted = CurrencyUtils.formatCentsToCurrency(125050L)
        // Deve conter 1.250,50 e R$
        assertTrue(formatted.contains("1.250,50") || formatted.contains("1250,50"))
        assertTrue(formatted.contains("R$"))
    }

    @Test
    fun formatCentsToDecimal_formatsProperly() {
        val decimal = CurrencyUtils.formatCentsToDecimal(4590L)
        assertEquals("45,90", decimal)
    }
}
