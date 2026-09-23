package com.example

import com.example.data.model.CardInvoicePaymentEntity
import com.example.data.model.CardType
import com.example.data.model.CreditCardEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.domain.calculator.CreditCardCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CreditCardCalculatorTest {

    @Test
    fun testDetermineInvoiceMonthYear_beforeClosingDay() {
        // May 10, closing day 20 -> cycle should be "2026-05"
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 10, 12, 0)
        }
        val invoice = CreditCardCalculator.getInvoiceCycleForPurchase(cal.timeInMillis, closingDay = 20)
        assertEquals("2026-05", invoice)
    }

    @Test
    fun testDetermineInvoiceMonthYear_onOrAfterClosingDay() {
        // May 20, closing day 20 -> falls into next cycle "2026-05" is on effective closing day so 20 falls in cycle 05, day 21 goes to 06
        val cal20 = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 20, 12, 0)
        }
        assertEquals("2026-05", CreditCardCalculator.getInvoiceCycleForPurchase(cal20.timeInMillis, closingDay = 20))

        val cal21 = Calendar.getInstance().apply {
            set(2026, Calendar.MAY, 21, 12, 0)
        }
        assertEquals("2026-06", CreditCardCalculator.getInvoiceCycleForPurchase(cal21.timeInMillis, closingDay = 20))
    }

    @Test
    fun testDetermineInvoiceMonthYear_decemberToJanuaryTransition() {
        // Dec 25, closing day 20 -> next cycle is January of next year "2027-01"
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.DECEMBER, 25, 12, 0)
        }
        val invoice = CreditCardCalculator.getInvoiceCycleForPurchase(cal.timeInMillis, closingDay = 20)
        assertEquals("2027-01", invoice)
    }

    @Test
    fun testDeterministicCentsDistribution_100ReaisIn3Installments() {
        // 100.00 BRL = 10,000 cents in 3 installments
        // 10,000 / 3 = 3,333 remainder 1.
        // First installment: 3,334 cents (R$ 33,34)
        // Second installment: 3,333 cents (R$ 33,33)
        // Third installment: 3,333 cents (R$ 33,33)
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JUNE, 5, 10, 0)
        }
        val previews = CreditCardCalculator.generateInstallmentPreviews(
            totalAmountCents = 10000L,
            installmentCount = 3,
            purchaseDateMillis = cal.timeInMillis,
            closingDay = 15
        )

        assertEquals(3, previews.size)
        assertEquals(3334L, previews[0].amountCents)
        assertEquals(3333L, previews[1].amountCents)
        assertEquals(3333L, previews[2].amountCents)

        // Sum must be exact
        val totalSum = previews.sumOf { it.amountCents }
        assertEquals(10000L, totalSum)

        // Invoices must advance month by month
        assertEquals("2026-06", previews[0].invoiceMonthYear)
        assertEquals("2026-07", previews[1].invoiceMonthYear)
        assertEquals("2026-08", previews[2].invoiceMonthYear)
    }

    @Test
    fun testCardSummaryCalculations() {
        val card = CreditCardEntity(
            id = 1L,
            name = "Nubank",
            type = CardType.CREDIT,
            issuer = "Nubank",
            creditLimitCents = 200000L, // R$ 2.000,00
            closingDay = 15,
            dueDay = 22
        )

        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 10, 12, 0)
        }

        val tx1 = TransactionEntity(
            id = 101L,
            description = "Supermercado",
            amountCents = 50000L, // R$ 500,00
            type = TransactionType.EXPENSE,
            categoryId = 1L,
            categoryName = "Alimentação",
            dateMillis = cal.timeInMillis,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            cardId = 1L,
            cardName = "Nubank",
            invoiceMonthYear = "2026-09"
        )

        val tx2 = TransactionEntity(
            id = 102L,
            description = "Notebook (1/2)",
            amountCents = 80000L, // R$ 800,00
            type = TransactionType.EXPENSE,
            categoryId = 2L,
            categoryName = "Tecnologia",
            dateMillis = cal.timeInMillis,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            cardId = 1L,
            cardName = "Nubank",
            invoiceMonthYear = "2026-09"
        )

        val summary = CreditCardCalculator.calculateCardSummary(
            card = card,
            allTransactions = listOf(tx1, tx2),
            payments = emptyList(),
            referenceMillis = cal.timeInMillis
        )

        assertEquals(200000L, summary.totalLimitCents)
        assertEquals(130000L, summary.usedLimitCents)
        assertEquals(70000L, summary.availableLimitCents)
        assertFalse(summary.isLimitExceeded)
        assertEquals(130000L, summary.currentInvoiceAmountCents)
        assertFalse(summary.isCurrentInvoicePaid)
    }

    @Test
    fun testLimitExceededCalculation() {
        val card = CreditCardEntity(
            id = 2L,
            name = "Inter",
            type = CardType.CREDIT,
            issuer = "Banco Inter",
            creditLimitCents = 100000L, // R$ 1.000,00
            closingDay = 10,
            dueDay = 17
        )

        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 5, 12, 0)
        }

        val tx = TransactionEntity(
            id = 201L,
            description = "Viagem",
            amountCents = 150000L, // R$ 1.500,00
            type = TransactionType.EXPENSE,
            categoryId = 5L,
            categoryName = "Viagem",
            dateMillis = cal.timeInMillis,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            cardId = 2L,
            cardName = "Inter",
            invoiceMonthYear = "2026-09"
        )

        val summary = CreditCardCalculator.calculateCardSummary(
            card = card,
            allTransactions = listOf(tx),
            payments = emptyList(),
            referenceMillis = cal.timeInMillis
        )

        assertEquals(100000L, summary.totalLimitCents)
        assertEquals(150000L, summary.usedLimitCents)
        assertEquals(-50000L, summary.availableLimitCents)
        assertTrue(summary.isLimitExceeded)
    }

    @Test
    fun testPaidInvoiceReleasesLimit() {
        val card = CreditCardEntity(
            id = 1L,
            name = "Nubank",
            type = CardType.CREDIT,
            issuer = "Nubank",
            creditLimitCents = 200000L,
            closingDay = 15,
            dueDay = 22
        )

        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 10, 12, 0)
        }

        val tx = TransactionEntity(
            id = 101L,
            description = "Supermercado",
            amountCents = 50000L,
            type = TransactionType.EXPENSE,
            categoryId = 1L,
            categoryName = "Alimentação",
            dateMillis = cal.timeInMillis,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            cardId = 1L,
            cardName = "Nubank",
            invoiceMonthYear = "2026-09"
        )

        val payment = CardInvoicePaymentEntity(
            cardId = 1L,
            invoiceMonthYear = "2026-09",
            amountPaidCents = 50000L,
            paymentDateMillis = cal.timeInMillis,
            isPaid = true
        )

        val summary = CreditCardCalculator.calculateCardSummary(
            card = card,
            allTransactions = listOf(tx),
            payments = listOf(payment),
            referenceMillis = cal.timeInMillis
        )

        assertEquals(0L, summary.usedLimitCents)
        assertEquals(200000L, summary.availableLimitCents)
        assertTrue(summary.isCurrentInvoicePaid)
    }
}
