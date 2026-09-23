package com.example.domain.calculator

import com.example.data.model.CardInvoicePaymentEntity
import com.example.data.model.CreditCardEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.domain.model.CardSummary
import com.example.domain.model.InstallmentPreview
import com.example.domain.model.InvoiceSummary
import java.util.Calendar
import java.util.Locale

object CreditCardCalculator {

    /**
     * Retorna o dia efetivo do mês, ajustando para o último dia válido em meses mais curtos
     * (ex.: dia 31 em abril vira 30; dia 31 em fevereiro vira 28 ou 29 se bissexto).
     */
    fun getEffectiveDayOfMonth(year: Int, month: Int, desiredDay: Int): Int {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return desiredDay.coerceIn(1, maxDays)
    }

    /**
     * Adiciona N meses a um ciclo (ano, mês 1..12).
     */
    fun addMonthsToCycle(year: Int, month: Int, monthsToAdd: Int): Pair<Int, Int> {
        val totalMonths = (year * 12 + (month - 1)) + monthsToAdd
        val newYear = totalMonths / 12
        val newMonth = (totalMonths % 12) + 1
        return Pair(newYear, newMonth)
    }

    /**
     * Formata o ciclo de fatura para formato padrão "YYYY-MM" (ex: "2026-09").
     */
    fun formatCycle(year: Int, month: Int): String {
        return String.format(Locale.ROOT, "%04d-%02d", year, month)
    }

    /**
     * Extrai ano e mês a partir da string "YYYY-MM".
     */
    fun parseCycle(cycle: String): Pair<Int, Int> {
        val parts = cycle.split("-")
        return if (parts.size == 2) {
            val y = parts[0].toIntOrNull() ?: 2026
            val m = parts[1].toIntOrNull() ?: 1
            Pair(y, m)
        } else {
            Pair(2026, 1)
        }
    }

    /**
     * Determina o ciclo da fatura correspondente a uma data de compra e o dia de fechamento:
     * - Compra realizada ATÉ o dia do fechamento (inclusive) entra na fatura daquele mês/ciclo.
     * - Compra realizada APÓS o fechamento entra na fatura do mês/ciclo seguinte.
     */
    fun getInvoiceCycleForPurchase(purchaseDateMillis: Long, closingDay: Int): String {
        val cal = Calendar.getInstance().apply { timeInMillis = purchaseDateMillis }
        val pYear = cal.get(Calendar.YEAR)
        val pMonth = cal.get(Calendar.MONTH) + 1
        val pDay = cal.get(Calendar.DAY_OF_MONTH)

        val effectiveClosingDay = getEffectiveDayOfMonth(pYear, pMonth, closingDay)

        val (cycleYear, cycleMonth) = if (pDay <= effectiveClosingDay) {
            Pair(pYear, pMonth)
        } else {
            addMonthsToCycle(pYear, pMonth, 1)
        }

        return formatCycle(cycleYear, cycleMonth)
    }

    /**
     * Retorna o timestamp de fechamento (final do dia) para determinado ciclo.
     */
    fun getClosingDateMillis(cycleYear: Int, cycleMonth: Int, closingDay: Int): Long {
        val effectiveDay = getEffectiveDayOfMonth(cycleYear, cycleMonth, closingDay)
        val cal = Calendar.getInstance().apply {
            clear()
            set(cycleYear, cycleMonth - 1, effectiveDay, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    /**
     * Retorna o timestamp de vencimento (final do dia) para determinado ciclo:
     * - Se dueDay > closingDay: vence no mesmo mês calendário do fechamento.
     * - Se dueDay <= closingDay: vence no mês calendário seguinte ao fechamento.
     */
    fun getDueDateMillis(cycleYear: Int, cycleMonth: Int, closingDay: Int, dueDay: Int): Long {
        val (dueYear, dueMonth) = if (dueDay > closingDay) {
            Pair(cycleYear, cycleMonth)
        } else {
            addMonthsToCycle(cycleYear, cycleMonth, 1)
        }

        val effectiveDueDay = getEffectiveDayOfMonth(dueYear, dueMonth, dueDay)
        val cal = Calendar.getInstance().apply {
            clear()
            set(dueYear, dueMonth - 1, effectiveDueDay, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    /**
     * Distribui o valor total em centavos de forma determinística entre as parcelas,
     * garantindo que a soma seja 100% igual ao total informado.
     */
    fun calculateInstallmentAmounts(totalAmountCents: Long, installmentCount: Int): List<Long> {
        require(installmentCount >= 1) { "A quantidade de parcelas deve ser de no mínimo 1." }
        val baseAmount = totalAmountCents / installmentCount
        val remainder = (totalAmountCents % installmentCount).toInt()

        return List(installmentCount) { index ->
            if (index < remainder) baseAmount + 1L else baseAmount
        }
    }

    /**
     * Gera a lista de parcelas detalhadas para uma compra.
     */
    fun generateInstallmentPreviews(
        totalAmountCents: Long,
        installmentCount: Int,
        purchaseDateMillis: Long,
        closingDay: Int
    ): List<InstallmentPreview> {
        val amounts = calculateInstallmentAmounts(totalAmountCents, installmentCount)
        val firstCycle = getInvoiceCycleForPurchase(purchaseDateMillis, closingDay)
        val (firstYear, firstMonth) = parseCycle(firstCycle)

        val cal = Calendar.getInstance().apply { timeInMillis = purchaseDateMillis }
        val pDay = cal.get(Calendar.DAY_OF_MONTH)

        return amounts.mapIndexed { index, amount ->
            val (cycleYear, cycleMonth) = addMonthsToCycle(firstYear, firstMonth, index)
            val invoiceMonthYear = formatCycle(cycleYear, cycleMonth)

            // Data da parcela: avança mês a mês mantendo o dia aproximado
            val instCal = Calendar.getInstance().apply {
                timeInMillis = purchaseDateMillis
                add(Calendar.MONTH, index)
            }

            InstallmentPreview(
                installmentNumber = index + 1,
                totalInstallments = installmentCount,
                amountCents = amount,
                dateMillis = instCal.timeInMillis,
                invoiceMonthYear = invoiceMonthYear
            )
        }
    }

    /**
     * Calcula o resumo de um cartão de crédito:
     * - Apenas compras no CRÉDITO do cartão afetam o limite e as faturas.
     * - Limite utilizado = soma das compras/parcelas cujas faturas AINDA NÃO FORAM PAGAS.
     * - Fatura paga libera exatamente o limite correspondente ao valor quitado.
     * - Limite disponível = limite total - limite utilizado.
     */
    fun calculateCardSummary(
        card: CreditCardEntity,
        allTransactions: List<TransactionEntity>,
        payments: List<CardInvoicePaymentEntity>,
        referenceMillis: Long = System.currentTimeMillis()
    ): CardSummary {
        val creditTransactions = allTransactions.filter {
            it.cardId == card.id &&
                it.type == TransactionType.EXPENSE &&
                it.paymentMethod == PaymentMethod.CREDIT_CARD
        }

        val paidInvoicesSet = payments.filter { it.cardId == card.id && it.isPaid }
            .map { it.invoiceMonthYear }
            .toSet()

        // Limite utilizado: apenas compras de faturas não pagas
        val usedLimitCents = creditTransactions.filterNot { it.invoiceMonthYear in paidInvoicesSet }
            .sumOf { it.amountCents }

        val availableLimitCents = card.creditLimitCents - usedLimitCents
        val isLimitExceeded = usedLimitCents > card.creditLimitCents

        // Determina a fatura atual
        val currentCycle = getInvoiceCycleForPurchase(referenceMillis, card.closingDay)
        val (cycleYear, cycleMonth) = parseCycle(currentCycle)

        val currentInvoiceAmountCents = creditTransactions
            .filter { it.invoiceMonthYear == currentCycle }
            .sumOf { it.amountCents }

        val isCurrentInvoicePaid = currentCycle in paidInvoicesSet

        val nextClosingDateMillis = getClosingDateMillis(cycleYear, cycleMonth, card.closingDay)
        val nextDueDateMillis = getDueDateMillis(cycleYear, cycleMonth, card.closingDay, card.dueDay)

        return CardSummary(
            card = card,
            totalLimitCents = card.creditLimitCents,
            usedLimitCents = usedLimitCents,
            availableLimitCents = availableLimitCents,
            isLimitExceeded = isLimitExceeded,
            currentInvoiceMonthYear = currentCycle,
            currentInvoiceAmountCents = currentInvoiceAmountCents,
            isCurrentInvoicePaid = isCurrentInvoicePaid,
            nextClosingDateMillis = nextClosingDateMillis,
            nextDueDateMillis = nextDueDateMillis
        )
    }

    /**
     * Agrupa as transações em faturas para exibição de histórico (passadas, atual, futuras).
     */
    fun calculateInvoicesForCard(
        card: CreditCardEntity,
        allTransactions: List<TransactionEntity>,
        payments: List<CardInvoicePaymentEntity>,
        referenceMillis: Long = System.currentTimeMillis()
    ): List<InvoiceSummary> {
        val creditTransactions = allTransactions.filter {
            it.cardId == card.id &&
                it.type == TransactionType.EXPENSE &&
                it.paymentMethod == PaymentMethod.CREDIT_CARD
        }

        val currentCycle = getInvoiceCycleForPurchase(referenceMillis, card.closingDay)
        val paymentMap = payments.filter { it.cardId == card.id }.associateBy { it.invoiceMonthYear }

        // Agrupa todas as transações por ciclo de fatura
        val grouped = creditTransactions.groupBy { it.invoiceMonthYear }

        // Garante que o ciclo atual e os próximos 2 ciclos apareçam mesmo que vazios
        val allCycles = (grouped.keys + currentCycle).toMutableSet()
        val (curYear, curMonth) = parseCycle(currentCycle)
        for (i in 1..2) {
            val (nextY, nextM) = addMonthsToCycle(curYear, curMonth, i)
            allCycles.add(formatCycle(nextY, nextM))
        }

        return allCycles.sorted().map { cycle ->
            val (year, month) = parseCycle(cycle)
            val closingDate = getClosingDateMillis(year, month, card.closingDay)
            val dueDate = getDueDateMillis(year, month, card.closingDay, card.dueDay)

            val txList = (grouped[cycle] ?: emptyList()).sortedBy { it.dateMillis }
            val totalAmount = txList.sumOf { it.amountCents }

            val payment = paymentMap[cycle]
            val isPaid = payment?.isPaid ?: false
            val paymentDate = payment?.paymentDateMillis
            val isClosed = referenceMillis > closingDate

            InvoiceSummary(
                monthYear = cycle,
                cardId = card.id,
                closingDateMillis = closingDate,
                dueDateMillis = dueDate,
                totalAmountCents = totalAmount,
                isPaid = isPaid,
                paymentDateMillis = paymentDate,
                isClosed = isClosed,
                transactions = txList
            )
        }
    }
}
