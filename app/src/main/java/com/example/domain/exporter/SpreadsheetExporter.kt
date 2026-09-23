package com.example.domain.exporter

import com.example.data.model.CardInvoicePaymentEntity
import com.example.data.model.CreditCardEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.domain.calculator.CreditCardCalculator
import com.example.domain.util.DateUtils
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Gerador de planilhas no formato OpenXML (.xlsx) compatível com
 * Microsoft Excel, Google Sheets e LibreOffice Calc.
 *
 * Gera o arquivo ZIP padrão com folhas de cálculo XML nativas,
 * cabeçalhos estilizados, congelamento de painel, autofiltro,
 * células numéricas reais com formatação monetária (R$) e datas reais.
 */
object SpreadsheetExporter {

    private data class SheetDefinition(
        val name: String,
        val sheetId: Int,
        val rId: String,
        val fileName: String,
        val content: String
    )

    fun export(
        outputStream: OutputStream,
        options: SpreadsheetExportOptions,
        allTransactions: List<TransactionEntity>,
        filteredTransactions: List<TransactionEntity>,
        cards: List<CreditCardEntity>,
        payments: List<CardInvoicePaymentEntity>,
        referenceMillis: Long = System.currentTimeMillis()
    ) {
        // 1. Filtrar as movimentações conforme as opções
        val baseList = if (options.transactionScope == ExportTransactionScope.FILTERED) {
            filteredTransactions
        } else {
            allTransactions
        }

        val periodFiltered = filterByPeriod(baseList, options.periodScope, referenceMillis)
        val finalTransactions = filterByType(periodFiltered, options.typeScope).sortedByDescending { it.dateMillis }

        // 2. Determinar as abas que serão incluídas
        val sheets = mutableListOf<SheetDefinition>()
        var sheetIndex = 1

        // Aba 1: Movimentações (Sempre incluída)
        val movSheetContent = generateMovimentacoesSheetXml(finalTransactions, payments)
        sheets.add(
            SheetDefinition(
                name = "Movimentações",
                sheetId = sheetIndex,
                rId = "rId$sheetIndex",
                fileName = "sheet$sheetIndex.xml",
                content = movSheetContent
            )
        )
        sheetIndex++

        // Aba 2: Cartões (Se selecionado e houver cartões)
        if (options.includeCardsAndInvoices && cards.isNotEmpty()) {
            val cardsSheetContent = generateCardsSheetXml(cards, allTransactions, payments, referenceMillis)
            sheets.add(
                SheetDefinition(
                    name = "Cartões",
                    sheetId = sheetIndex,
                    rId = "rId$sheetIndex",
                    fileName = "sheet$sheetIndex.xml",
                    content = cardsSheetContent
                )
            )
            sheetIndex++
        }

        // Aba 3: Faturas e Parcelas (Se selecionado e houver despesas no crédito ou compras parceladas)
        if (options.includeCardsAndInvoices && cards.isNotEmpty()) {
            val creditTransactions = allTransactions.filter {
                it.paymentMethod == PaymentMethod.CREDIT_CARD || it.cardId != null || it.installmentGroupId != null
            }.sortedWith(compareBy({ it.cardName }, { it.invoiceMonthYear }, { it.dateMillis }))

            if (creditTransactions.isNotEmpty()) {
                val invoicesSheetContent = generateInvoicesAndInstallmentsSheetXml(creditTransactions, cards, payments)
                sheets.add(
                    SheetDefinition(
                        name = "Faturas e Parcelas",
                        sheetId = sheetIndex,
                        rId = "rId$sheetIndex",
                        fileName = "sheet$sheetIndex.xml",
                        content = invoicesSheetContent
                    )
                )
                sheetIndex++
            }
        }

        // 3. Compactar em formato ZIP .xlsx
        ZipOutputStream(outputStream).use { zip ->
            // [Content_Types].xml
            writeZipEntry(zip, "[Content_Types].xml", generateContentTypesXml(sheets))

            // _rels/.rels
            writeZipEntry(zip, "_rels/.rels", generatePackageRelsXml())

            // docProps/core.xml & docProps/app.xml
            val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date(referenceMillis))
            writeZipEntry(zip, "docProps/core.xml", generateCorePropsXml(nowIso))
            writeZipEntry(zip, "docProps/app.xml", generateAppPropsXml())

            // xl/workbook.xml & xl/_rels/workbook.xml.rels
            writeZipEntry(zip, "xl/workbook.xml", generateWorkbookXml(sheets))
            writeZipEntry(zip, "xl/_rels/workbook.xml.rels", generateWorkbookRelsXml(sheets))

            // xl/styles.xml
            writeZipEntry(zip, "xl/styles.xml", generateStylesXml())

            // xl/worksheets/sheetX.xml
            for (sheet in sheets) {
                writeZipEntry(zip, "xl/worksheets/${sheet.fileName}", sheet.content)
            }
            zip.flush()
        }
    }

    private fun filterByPeriod(
        transactions: List<TransactionEntity>,
        scope: ExportPeriodScope,
        referenceMillis: Long
    ): List<TransactionEntity> {
        val cal = Calendar.getInstance().apply { timeInMillis = referenceMillis }
        return when (scope) {
            ExportPeriodScope.ALL -> transactions
            ExportPeriodScope.CURRENT_MONTH -> {
                val curMonthYear = DateUtils.formatToMonthYear(cal)
                val (start, end) = DateUtils.getMonthRangeMillis(curMonthYear)
                transactions.filter { it.dateMillis in start..end }
            }
            ExportPeriodScope.LAST_THREE_MONTHS -> {
                val endCal = Calendar.getInstance().apply {
                    timeInMillis = referenceMillis
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val startCal = Calendar.getInstance().apply {
                    timeInMillis = referenceMillis
                    add(Calendar.MONTH, -2)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                transactions.filter { it.dateMillis in startCal.timeInMillis..endCal.timeInMillis }
            }
            ExportPeriodScope.CURRENT_YEAR -> {
                val curYear = cal.get(Calendar.YEAR)
                transactions.filter {
                    val txCal = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
                    txCal.get(Calendar.YEAR) == curYear
                }
            }
        }
    }

    private fun filterByType(
        transactions: List<TransactionEntity>,
        scope: ExportTypeScope
    ): List<TransactionEntity> {
        return when (scope) {
            ExportTypeScope.ALL -> transactions
            ExportTypeScope.EXPENSE_ONLY -> transactions.filter { it.type == TransactionType.EXPENSE }
            ExportTypeScope.INCOME_ONLY -> transactions.filter { it.type == TransactionType.INCOME }
        }
    }

    // =========================================================================
    // GERAÇÃO DOS XMLs DAS ABAS
    // =========================================================================

    private fun generateMovimentacoesSheetXml(
        transactions: List<TransactionEntity>,
        payments: List<CardInvoicePaymentEntity>
    ): String {
        val paidSet = payments.filter { it.isPaid }.map { "${it.cardId}_${it.invoiceMonthYear}" }.toSet()
        val sb = StringBuilder()

        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")

        // Congelar primeira linha (cabeçalho)
        sb.append("""<sheetViews><sheetView tabSelected="1" workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>""")
        sb.append("""<sheetFormatPr defaultRowHeight="20"/>""")

        // Larguras de colunas adequadas
        sb.append("""<cols>""")
        sb.append("""<col min="1" max="1" width="14" customWidth="1"/>""") // Data
        sb.append("""<col min="2" max="2" width="30" customWidth="1"/>""") // Descrição
        sb.append("""<col min="3" max="3" width="22" customWidth="1"/>""") // Categoria
        sb.append("""<col min="4" max="4" width="14" customWidth="1"/>""") // Tipo
        sb.append("""<col min="5" max="5" width="20" customWidth="1"/>""") // Forma de Pagamento
        sb.append("""<col min="6" max="6" width="20" customWidth="1"/>""") // Cartão Utilizado
        sb.append("""<col min="7" max="7" width="18" customWidth="1"/>""") // Valor
        sb.append("""<col min="8" max="8" width="18" customWidth="1"/>""") // Situação
        sb.append("""<col min="9" max="9" width="30" customWidth="1"/>""") // Observações
        sb.append("""</cols>""")

        sb.append("""<sheetData>""")

        // Linha 1: Cabeçalhos
        val headers = listOf(
            "Data", "Descrição", "Categoria", "Tipo",
            "Forma de Pagamento", "Cartão Utilizado", "Valor (R$)", "Situação", "Observações"
        )
        sb.append("""<row r="1" ht="26" customHeight="1">""")
        headers.forEachIndexed { i, h ->
            val colLetter = toColumnLetter(i)
            sb.append("""<c r="${colLetter}1" s="1" t="inlineStr"><is><t>${escapeXml(h)}</t></is></c>""")
        }
        sb.append("""</row>""")

        var totalIncomeCents = 0L
        var totalExpenseCents = 0L

        // Linhas de dados (linhas 2 .. N+1)
        transactions.forEachIndexed { index, tx ->
            val rowNum = index + 2
            if (tx.type == TransactionType.INCOME) {
                totalIncomeCents += tx.amountCents
            } else {
                totalExpenseCents += tx.amountCents
            }

            val excelDate = millisToExcelDate(tx.dateMillis)
            val desc = tx.description
            val cat = tx.categoryName
            val tipo = if (tx.type == TransactionType.INCOME) "Receita" else "Despesa"
            val forma = tx.paymentMethod.displayName
            val cartao = tx.cardName ?: "-"
            val valorDouble = String.format(Locale.US, "%.2f", tx.amountCents.toDouble() / 100.0)

            val situacao = if (tx.paymentMethod == PaymentMethod.CREDIT_CARD) {
                val key = "${tx.cardId}_${tx.invoiceMonthYear}"
                if (key in paidSet) "Fatura Paga" else "Fatura Aberta (${tx.invoiceMonthYear ?: "Atual"})"
            } else {
                "Realizado"
            }
            val obs = if (!tx.note.isNullOrBlank()) tx.note else "-"

            sb.append("""<row r="$rowNum" ht="20" customHeight="1">""")
            sb.append("""<c r="A$rowNum" s="2"><v>$excelDate</v></c>""")
            sb.append("""<c r="B$rowNum" s="0" t="inlineStr"><is><t>${escapeXml(desc)}</t></is></c>""")
            sb.append("""<c r="C$rowNum" s="0" t="inlineStr"><is><t>${escapeXml(cat)}</t></is></c>""")
            sb.append("""<c r="D$rowNum" s="4" t="inlineStr"><is><t>${escapeXml(tipo)}</t></is></c>""")
            sb.append("""<c r="E$rowNum" s="4" t="inlineStr"><is><t>${escapeXml(forma)}</t></is></c>""")
            sb.append("""<c r="F$rowNum" s="0" t="inlineStr"><is><t>${escapeXml(cartao)}</t></is></c>""")
            sb.append("""<c r="G$rowNum" s="3"><v>$valorDouble</v></c>""")
            sb.append("""<c r="H$rowNum" s="4" t="inlineStr"><is><t>${escapeXml(situacao)}</t></is></c>""")
            sb.append("""<c r="I$rowNum" s="0" t="inlineStr"><is><t>${escapeXml(obs)}</t></is></c>""")
            sb.append("""</row>""")
        }

        val lastDataRow = transactions.size + 1

        // Linhas de Totalização
        val totalIncomeDouble = String.format(Locale.US, "%.2f", totalIncomeCents.toDouble() / 100.0)
        val totalExpenseDouble = String.format(Locale.US, "%.2f", totalExpenseCents.toDouble() / 100.0)
        val netBalanceDouble = String.format(Locale.US, "%.2f", (totalIncomeCents - totalExpenseCents).toDouble() / 100.0)

        val rEmpty = lastDataRow + 1
        val rIncome = lastDataRow + 2
        val rExpense = lastDataRow + 3
        val rNet = lastDataRow + 4

        // Linha em branco separadora
        sb.append("""<row r="$rEmpty" ht="12" customHeight="1"/>""")

        // Linha Total de Receitas
        sb.append("""<row r="$rIncome" ht="22" customHeight="1">""")
        sb.append("""<c r="F$rIncome" s="6" t="inlineStr"><is><t>Total de Receitas</t></is></c>""")
        if (transactions.isNotEmpty()) {
            sb.append("""<c r="G$rIncome" s="7"><f>SUMIF(D2:D$lastDataRow,"Receita",G2:G$lastDataRow)</f><v>$totalIncomeDouble</v></c>""")
        } else {
            sb.append("""<c r="G$rIncome" s="7"><v>0.00</v></c>""")
        }
        sb.append("""</row>""")

        // Linha Total de Despesas
        sb.append("""<row r="$rExpense" ht="22" customHeight="1">""")
        sb.append("""<c r="F$rExpense" s="6" t="inlineStr"><is><t>Total de Despesas</t></is></c>""")
        if (transactions.isNotEmpty()) {
            sb.append("""<c r="G$rExpense" s="7"><f>SUMIF(D2:D$lastDataRow,"Despesa",G2:G$lastDataRow)</f><v>$totalExpenseDouble</v></c>""")
        } else {
            sb.append("""<c r="G$rExpense" s="7"><v>0.00</v></c>""")
        }
        sb.append("""</row>""")

        // Linha Saldo Líquido
        sb.append("""<row r="$rNet" ht="24" customHeight="1">""")
        sb.append("""<c r="F$rNet" s="6" t="inlineStr"><is><t>Saldo Líquido</t></is></c>""")
        if (transactions.isNotEmpty()) {
            sb.append("""<c r="G$rNet" s="7"><f>G$rIncome-G$rExpense</f><v>$netBalanceDouble</v></c>""")
        } else {
            sb.append("""<c r="G$rNet" s="7"><v>0.00</v></c>""")
        }
        sb.append("""</row>""")

        sb.append("""</sheetData>""")

        // AutoFiltro na linha de cabeçalho
        sb.append("""<autoFilter ref="A1:I1"/>""")
        sb.append("""</worksheet>""")

        return sb.toString()
    }

    private fun generateCardsSheetXml(
        cards: List<CreditCardEntity>,
        allTransactions: List<TransactionEntity>,
        payments: List<CardInvoicePaymentEntity>,
        referenceMillis: Long
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("""<sheetViews><sheetView tabSelected="1" workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>""")
        sb.append("""<sheetFormatPr defaultRowHeight="20"/>""")

        sb.append("""<cols>""")
        sb.append("""<col min="1" max="1" width="22" customWidth="1"/>""") // Nome
        sb.append("""<col min="2" max="2" width="16" customWidth="1"/>""") // Tipo
        sb.append("""<col min="3" max="3" width="20" customWidth="1"/>""") // Emissor
        sb.append("""<col min="4" max="4" width="16" customWidth="1"/>""") // Bandeira
        sb.append("""<col min="5" max="5" width="16" customWidth="1"/>""") // Últimos Dígitos
        sb.append("""<col min="6" max="6" width="18" customWidth="1"/>""") // Limite Total
        sb.append("""<col min="7" max="7" width="18" customWidth="1"/>""") // Limite Utilizado
        sb.append("""<col min="8" max="8" width="18" customWidth="1"/>""") // Limite Disponível
        sb.append("""<col min="9" max="9" width="16" customWidth="1"/>""") // Dia Fechamento
        sb.append("""<col min="10" max="10" width="16" customWidth="1"/>""") // Dia Vencimento
        sb.append("""<col min="11" max="11" width="14" customWidth="1"/>""") // Status
        sb.append("""</cols>""")

        sb.append("""<sheetData>""")

        val headers = listOf(
            "Nome do Cartão", "Tipo", "Banco / Emissor", "Bandeira",
            "Últimos 4 Dígitos", "Limite Total (R$)", "Limite Utilizado (R$)", "Limite Disponível (R$)",
            "Dia Fechamento", "Dia Vencimento", "Status"
        )
        sb.append("""<row r="1" ht="26" customHeight="1">""")
        headers.forEachIndexed { i, h ->
            val col = toColumnLetter(i)
            sb.append("""<c r="${col}1" s="1" t="inlineStr"><is><t>${escapeXml(h)}</t></is></c>""")
        }
        sb.append("""</row>""")

        cards.forEachIndexed { index, card ->
            val rowNum = index + 2
            val summary = CreditCardCalculator.calculateCardSummary(card, allTransactions, payments, referenceMillis)

            val limitTotal = String.format(Locale.US, "%.2f", card.creditLimitCents.toDouble() / 100.0)
            val limitUsed = String.format(Locale.US, "%.2f", summary.usedLimitCents.toDouble() / 100.0)
            val limitAvail = String.format(Locale.US, "%.2f", summary.availableLimitCents.toDouble() / 100.0)
            val digits = if (card.lastFourDigits.isNotBlank()) card.lastFourDigits else "-"
            val network = if (card.network.isNotBlank()) card.network else "-"
            val status = if (card.isArchived) "Arquivado" else "Ativo"

            sb.append("""<row r="$rowNum" ht="20" customHeight="1">""")
            sb.append("""<c r="A$rowNum" s="0" t="inlineStr"><is><t>${escapeXml(card.name)}</t></is></c>""")
            sb.append("""<c r="B$rowNum" s="4" t="inlineStr"><is><t>${escapeXml(card.type.displayName)}</t></is></c>""")
            sb.append("""<c r="C$rowNum" s="0" t="inlineStr"><is><t>${escapeXml(card.issuer)}</t></is></c>""")
            sb.append("""<c r="D$rowNum" s="4" t="inlineStr"><is><t>${escapeXml(network)}</t></is></c>""")
            sb.append("""<c r="E$rowNum" s="4" t="inlineStr"><is><t>${escapeXml(digits)}</t></is></c>""")
            sb.append("""<c r="F$rowNum" s="3"><v>$limitTotal</v></c>""")
            sb.append("""<c r="G$rowNum" s="3"><v>$limitUsed</v></c>""")
            sb.append("""<c r="H$rowNum" s="3"><v>$limitAvail</v></c>""")
            sb.append("""<c r="I$rowNum" s="5"><v>${card.closingDay}</v></c>""")
            sb.append("""<c r="J$rowNum" s="5"><v>${card.dueDay}</v></c>""")
            sb.append("""<c r="K$rowNum" s="4" t="inlineStr"><is><t>${escapeXml(status)}</t></is></c>""")
            sb.append("""</row>""")
        }

        sb.append("""</sheetData>""")
        sb.append("""<autoFilter ref="A1:K1"/>""")
        sb.append("""</worksheet>""")
        return sb.toString()
    }

    private fun generateInvoicesAndInstallmentsSheetXml(
        creditTransactions: List<TransactionEntity>,
        cards: List<CreditCardEntity>,
        payments: List<CardInvoicePaymentEntity>
    ): String {
        val cardMap = cards.associateBy { it.id }
        val paymentMap = payments.associateBy { "${it.cardId}_${it.invoiceMonthYear}" }

        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("""<sheetViews><sheetView tabSelected="1" workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>""")
        sb.append("""<sheetFormatPr defaultRowHeight="20"/>""")

        sb.append("""<cols>""")
        sb.append("""<col min="1" max="1" width="22" customWidth="1"/>""") // Cartão
        sb.append("""<col min="2" max="2" width="28" customWidth="1"/>""") // Descrição da Compra
        sb.append("""<col min="3" max="3" width="16" customWidth="1"/>""") // Nº Parcela
        sb.append("""<col min="4" max="4" width="16" customWidth="1"/>""") // Total Parcelas
        sb.append("""<col min="5" max="5" width="18" customWidth="1"/>""") // Valor da Parcela
        sb.append("""<col min="6" max="6" width="18" customWidth="1"/>""") // Fatura / Mês
        sb.append("""<col min="7" max="7" width="16" customWidth="1"/>""") // Vencimento
        sb.append("""<col min="8" max="8" width="16" customWidth="1"/>""") // Situação
        sb.append("""<col min="9" max="9" width="18" customWidth="1"/>""") // Data Pagamento
        sb.append("""</cols>""")

        sb.append("""<sheetData>""")

        val headers = listOf(
            "Cartão", "Descrição da Compra", "Nº Parcela", "Total Parcelas",
            "Valor da Parcela (R$)", "Competência Fatura", "Vencimento", "Situação", "Data Pagamento"
        )
        sb.append("""<row r="1" ht="26" customHeight="1">""")
        headers.forEachIndexed { i, h ->
            val col = toColumnLetter(i)
            sb.append("""<c r="${col}1" s="1" t="inlineStr"><is><t>${escapeXml(h)}</t></is></c>""")
        }
        sb.append("""</row>""")

        var totalInstallmentsCents = 0L

        creditTransactions.forEachIndexed { index, tx ->
            val rowNum = index + 2
            totalInstallmentsCents += tx.amountCents

            val card = tx.cardId?.let { cardMap[it] }
            val cardName = tx.cardName ?: card?.name ?: "-"
            val desc = tx.description
            val numParcela = tx.installmentNumber ?: 1
            val totParcelas = tx.totalInstallments ?: 1
            val amountDouble = String.format(Locale.US, "%.2f", tx.amountCents.toDouble() / 100.0)
            val competencia = tx.invoiceMonthYear ?: "-"

            val dueDateStr = if (card != null && tx.invoiceMonthYear != null) {
                val (y, m) = CreditCardCalculator.parseCycle(tx.invoiceMonthYear)
                val dueMillis = CreditCardCalculator.getDueDateMillis(y, m, card.closingDay, card.dueDay)
                DateUtils.formatDate(dueMillis)
            } else {
                "-"
            }

            val payKey = "${tx.cardId}_${tx.invoiceMonthYear}"
            val payment = paymentMap[payKey]
            val isPaid = payment?.isPaid == true

            val situacao = if (isPaid) "Fatura Paga" else "Fatura Aberta"
            val payDateStr = if (isPaid && payment?.paymentDateMillis != null && payment.paymentDateMillis > 0L) {
                DateUtils.formatDate(payment.paymentDateMillis)
            } else {
                "-"
            }

            sb.append("""<row r="$rowNum" ht="20" customHeight="1">""")
            sb.append("""<c r="A$rowNum" s="0" t="inlineStr"><is><t>${escapeXml(cardName)}</t></is></c>""")
            sb.append("""<c r="B$rowNum" s="0" t="inlineStr"><is><t>${escapeXml(desc)}</t></is></c>""")
            sb.append("""<c r="C$rowNum" s="5"><v>$numParcela</v></c>""")
            sb.append("""<c r="D$rowNum" s="5"><v>$totParcelas</v></c>""")
            sb.append("""<c r="E$rowNum" s="3"><v>$amountDouble</v></c>""")
            sb.append("""<c r="F$rowNum" s="4" t="inlineStr"><is><t>${escapeXml(competencia)}</t></is></c>""")
            sb.append("""<c r="G$rowNum" s="4" t="inlineStr"><is><t>${escapeXml(dueDateStr)}</t></is></c>""")
            sb.append("""<c r="H$rowNum" s="4" t="inlineStr"><is><t>${escapeXml(situacao)}</t></is></c>""")
            sb.append("""<c r="I$rowNum" s="4" t="inlineStr"><is><t>${escapeXml(payDateStr)}</t></is></c>""")
            sb.append("""</row>""")
        }

        val lastRow = creditTransactions.size + 1
        val totalDouble = String.format(Locale.US, "%.2f", totalInstallmentsCents.toDouble() / 100.0)
        val rTot = lastRow + 2

        // Linha de total
        sb.append("""<row r="$rTot" ht="22" customHeight="1">""")
        sb.append("""<c r="D$rTot" s="6" t="inlineStr"><is><t>Total de Lançamentos</t></is></c>""")
        if (creditTransactions.isNotEmpty()) {
            sb.append("""<c r="E$rTot" s="7"><f>SUM(E2:E$lastRow)</f><v>$totalDouble</v></c>""")
        } else {
            sb.append("""<c r="E$rTot" s="7"><v>0.00</v></c>""")
        }
        sb.append("""</row>""")

        sb.append("""</sheetData>""")
        sb.append("""<autoFilter ref="A1:I1"/>""")
        sb.append("""</worksheet>""")
        return sb.toString()
    }

    // =========================================================================
    // ESTRUTURA OPENXML (WORKBOOK, CONTENT_TYPES, RELS, STYLES)
    // =========================================================================

    private fun generateContentTypesXml(sheets: List<SheetDefinition>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        sb.append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        sb.append("""<Default Extension="xml" ContentType="application/xml"/>""")
        sb.append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
        sb.append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
        sb.append("""<Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>""")
        sb.append("""<Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>""")
        for (sheet in sheets) {
            sb.append("""<Override PartName="/xl/worksheets/${sheet.fileName}" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
        }
        sb.append("""</Types>""")
        return sb.toString()
    }

    private fun generatePackageRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
</Relationships>"""
    }

    private fun generateCorePropsXml(createdIso: String): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <dc:title>Gestão Financeira</dc:title>
  <dc:creator>Finanzo</dc:creator>
  <cp:lastModifiedBy>Finanzo</cp:lastModifiedBy>
  <dcterms:created xsi:type="dcterms:W3CDTF">$createdIso</dcterms:created>
  <dcterms:modified xsi:type="dcterms:W3CDTF">$createdIso</dcterms:modified>
</cp:coreProperties>"""
    }

    private fun generateAppPropsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties">
  <Application>Finanzo</Application>
</Properties>"""
    }

    private fun generateWorkbookXml(sheets: List<SheetDefinition>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
        sb.append("""<bookViews><workbookView xWindow="0" yWindow="0" windowWidth="20480" windowHeight="10240"/></bookViews>""")
        sb.append("""<sheets>""")
        for (sheet in sheets) {
            sb.append("""<sheet name="${escapeXml(sheet.name)}" sheetId="${sheet.sheetId}" r:id="${sheet.rId}"/>""")
        }
        sb.append("""</sheets>""")
        sb.append("""<calcPr calcId="144525"/>""")
        sb.append("""</workbook>""")
        return sb.toString()
    }

    private fun generateWorkbookRelsXml(sheets: List<SheetDefinition>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        sb.append("""<Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
        for (sheet in sheets) {
            sb.append("""<Relationship Id="${sheet.rId}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/${sheet.fileName}"/>""")
        }
        sb.append("""</Relationships>""")
        return sb.toString()
    }

    private fun generateStylesXml(): String {
        // Estilos:
        // 0: Texto normal com borda fina
        // 1: Cabeçalho (Fundo azul escuro #1E3A8A, texto branco em negrito, centralizado)
        // 2: Data (dd/mm/yyyy, centralizado com borda)
        // 3: Moeda (R$ #,##0.00 com borda fina, alinhado à direita)
        // 4: Texto centralizado com borda fina
        // 5: Inteiro centralizado com borda fina
        // 6: Rótulo de total (Negrito, fundo cinza #F3F4F6, borda contábil)
        // 7: Valor de total monetário (Negrito, fundo cinza, R$ #,##0.00, borda contábil)
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <numFmts count="2">
    <numFmt numFmtId="164" formatCode="&quot;R$&quot;\ #,##0.00;[Red]\-&quot;R$&quot;\ #,##0.00;&quot;R$&quot;\ 0.00"/>
    <numFmt numFmtId="165" formatCode="dd/mm/yyyy"/>
  </numFmts>
  <fonts count="3">
    <font>
      <sz val="11"/>
      <name val="Calibri"/>
      <family val="2"/>
    </font>
    <font>
      <b/>
      <sz val="11"/>
      <color rgb="FFFFFFFF"/>
      <name val="Calibri"/>
      <family val="2"/>
    </font>
    <font>
      <b/>
      <sz val="11"/>
      <color rgb="FF1F2937"/>
      <name val="Calibri"/>
      <family val="2"/>
    </font>
  </fonts>
  <fills count="4">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill>
      <patternFill patternType="solid">
        <fgColor rgb="FF1E3A8A"/>
        <bgColor indexed="64"/>
      </patternFill>
    </fill>
    <fill>
      <patternFill patternType="solid">
        <fgColor rgb="FFF3F4F6"/>
        <bgColor indexed="64"/>
      </patternFill>
    </fill>
  </fills>
  <borders count="3">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left style="thin"><color rgb="FFD1D5DB"/></left>
      <right style="thin"><color rgb="FFD1D5DB"/></right>
      <top style="thin"><color rgb="FFD1D5DB"/></top>
      <bottom style="thin"><color rgb="FFD1D5DB"/></bottom>
    </border>
    <border>
      <left style="thin"><color rgb="FFD1D5DB"/></left>
      <right style="thin"><color rgb="FFD1D5DB"/></right>
      <top style="thin"><color rgb="FF9CA3AF"/></top>
      <bottom style="double"><color rgb="FF111827"/></bottom>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="8">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="0" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="left" vertical="center"/>
    </xf>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyNumberFormat="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center" wrapText="0"/>
    </xf>
    <xf numFmtId="165" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="0" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <xf numFmtId="1" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <xf numFmtId="0" fontId="2" fillId="3" borderId="2" xfId="0" applyNumberFormat="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="left" vertical="center"/>
    </xf>
    <xf numFmtId="164" fontId="2" fillId="3" borderId="2" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
  </cellXfs>
</styleSheet>"""
    }

    // =========================================================================
    // UTILITÁRIOS
    // =========================================================================

    private fun writeZipEntry(zip: ZipOutputStream, entryName: String, content: String) {
        val entry = ZipEntry(entryName)
        zip.putNextEntry(entry)
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        zip.write(bytes)
        zip.closeEntry()
    }

    /**
     * Converte timestamp de milissegundos para o número serial de data do Excel (dias desde 30/12/1899).
     */
    fun millisToExcelDate(millis: Long): Int {
        val target = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            val localCal = Calendar.getInstance().apply { timeInMillis = millis }
            set(localCal.get(Calendar.YEAR), localCal.get(Calendar.MONTH), localCal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        }
        val excelEpoch = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(1899, Calendar.DECEMBER, 30, 0, 0, 0)
        }
        val diff = (target.timeInMillis - excelEpoch.timeInMillis) / (24L * 60L * 60L * 1000L)
        return diff.toInt().coerceAtLeast(1)
    }

    /**
     * Converte índice 0-base de coluna para letras de coluna do Excel (0 -> "A", 1 -> "B", ..., 26 -> "AA").
     */
    fun toColumnLetter(colIndex: Int): String {
        var num = colIndex
        val sb = StringBuilder()
        while (num >= 0) {
            sb.insert(0, ('A'.code + (num % 26)).toChar())
            num = (num / 26) - 1
        }
        return sb.toString()
    }

    /**
     * Escapa caracteres especiais XML (&, <, >, ", ').
     */
    fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    /**
     * Sugere o nome de arquivo padrão para salvar a planilha:
     * ex.: gestao-financeira-2026-09-22.xlsx
     */
    fun getSuggestedFileName(referenceMillis: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateStr = sdf.format(Date(referenceMillis))
        return "gestao-financeira-$dateStr.xlsx"
    }
}
