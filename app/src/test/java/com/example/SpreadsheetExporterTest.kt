package com.example

import com.example.data.model.CardInvoicePaymentEntity
import com.example.data.model.CardType
import com.example.data.model.CreditCardEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.domain.exporter.ExportPeriodScope
import com.example.domain.exporter.ExportTransactionScope
import com.example.domain.exporter.ExportTypeScope
import com.example.domain.exporter.SpreadsheetExportOptions
import com.example.domain.exporter.SpreadsheetExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Calendar
import java.util.zip.ZipInputStream

class SpreadsheetExporterTest {

    private val sampleCard = CreditCardEntity(
        id = 10L,
        name = "Cartão Nubank",
        type = CardType.CREDIT,
        issuer = "Nubank",
        network = "Mastercard",
        colorHex = 0xFF8A05BEL,
        lastFourDigits = "1234",
        creditLimitCents = 500000L, // R$ 5.000,00
        closingDay = 10,
        dueDay = 17
    )

    private val sampleTransactions = listOf(
        TransactionEntity(
            id = 1L,
            description = "Salário Empresa & Cia",
            amountCents = 600000L, // R$ 6.000,00
            type = TransactionType.INCOME,
            categoryId = 1L,
            categoryName = "Salário",
            dateMillis = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 5, 10, 0) }.timeInMillis,
            paymentMethod = PaymentMethod.PIX,
            note = "Pagamento mensal <regular>"
        ),
        TransactionEntity(
            id = 2L,
            description = "Supermercado \"Boa Compra\"",
            amountCents = 45050L, // R$ 450,50
            type = TransactionType.EXPENSE,
            categoryId = 2L,
            categoryName = "Alimentação",
            dateMillis = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 12, 18, 30) }.timeInMillis,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            cardId = 10L,
            cardName = "Cartão Nubank",
            installmentNumber = 1,
            totalInstallments = 2,
            installmentGroupId = "group-uuid-12345",
            invoiceMonthYear = "2026-10"
        ),
        TransactionEntity(
            id = 3L,
            description = "Restaurante Italiano",
            amountCents = 12000L, // R$ 120,00
            type = TransactionType.EXPENSE,
            categoryId = 2L,
            categoryName = "Alimentação",
            dateMillis = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 15, 20, 0) }.timeInMillis,
            paymentMethod = PaymentMethod.CASH
        )
    )

    private val samplePayments = listOf(
        CardInvoicePaymentEntity(
            id = 100L,
            cardId = 10L,
            invoiceMonthYear = "2026-09",
            isPaid = true,
            paymentDateMillis = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 16) }.timeInMillis,
            amountPaidCents = 35000L
        )
    )

    @Test
    fun testExportGeneratesValidZipPackageWithStandardEntries() {
        val out = ByteArrayOutputStream()
        SpreadsheetExporter.export(
            outputStream = out,
            options = SpreadsheetExportOptions(),
            allTransactions = sampleTransactions,
            filteredTransactions = sampleTransactions,
            cards = listOf(sampleCard),
            payments = samplePayments
        )

        val zipBytes = out.toByteArray()
        assertTrue("O arquivo ZIP deve conter dados", zipBytes.isNotEmpty())

        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val content = zis.readBytes().toString(Charsets.UTF_8)
                entries[entry.name] = content
                entry = zis.nextEntry
            }
        }

        // Verifica os arquivos obrigatórios da especificação OpenXML (.xlsx)
        assertTrue(entries.containsKey("[Content_Types].xml"))
        assertTrue(entries.containsKey("_rels/.rels"))
        assertTrue(entries.containsKey("docProps/core.xml"))
        assertTrue(entries.containsKey("docProps/app.xml"))
        assertTrue(entries.containsKey("xl/workbook.xml"))
        assertTrue(entries.containsKey("xl/_rels/workbook.xml.rels"))
        assertTrue(entries.containsKey("xl/styles.xml"))
        assertTrue(entries.containsKey("xl/worksheets/sheet1.xml"))
        assertTrue(entries.containsKey("xl/worksheets/sheet2.xml"))
        assertTrue(entries.containsKey("xl/worksheets/sheet3.xml"))

        // Verifica congelamento de painel e autofiltro na primeira planilha
        val sheet1Xml = entries["xl/worksheets/sheet1.xml"]!!
        assertTrue("Deve ter congelamento de painel na linha 1", sheet1Xml.contains("""state="frozen""""))
        assertTrue("Deve ter autoFilter nos cabeçalhos", sheet1Xml.contains("""<autoFilter ref="A1:I1"/>"""))

        // Verifica que valores monetários são números (não texto)
        assertTrue("Deve conter valor numérico formatado", sheet1Xml.contains("""<v>6000.00</v>"""))
        assertTrue("Deve conter valor numérico formatado", sheet1Xml.contains("""<v>450.50</v>"""))

        // Verifica presença de fórmulas SUMIF de totalização
        assertTrue("Deve conter fórmula de total de receitas", sheet1Xml.contains("""<f>SUMIF(D2:D4,"Receita",G2:G4)</f>"""))
        assertTrue("Deve conter fórmula de total de despesas", sheet1Xml.contains("""<f>SUMIF(D2:D4,"Despesa",G2:G4)</f>"""))

        // Verifica escape de XML
        assertTrue("Caracteres especiais devem ser escapados", sheet1Xml.contains("Salário Empresa &amp; Cia"))
        assertTrue("Caracteres especiais devem ser escapados", sheet1Xml.contains("&lt;regular&gt;"))
        assertTrue("Caracteres especiais devem ser escapados", sheet1Xml.contains("&quot;Boa Compra&quot;"))

        // NUNCA exportar identificadores internos (ex: UUID do grupo de parcelas)
        assertFalse("Não deve conter UUID de grupo de parcelamento", sheet1Xml.contains("group-uuid-12345"))
    }

    @Test
    fun testExportWithoutCardsExcludesAdditionalSheets() {
        val out = ByteArrayOutputStream()
        SpreadsheetExporter.export(
            outputStream = out,
            options = SpreadsheetExportOptions(includeCardsAndInvoices = false),
            allTransactions = sampleTransactions,
            filteredTransactions = sampleTransactions,
            cards = listOf(sampleCard),
            payments = samplePayments
        )

        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entries[entry.name] = zis.readBytes().toString(Charsets.UTF_8)
                entry = zis.nextEntry
            }
        }

        assertTrue(entries.containsKey("xl/worksheets/sheet1.xml"))
        assertFalse("Não deve incluir sheet2 quando cartões não forem selecionados", entries.containsKey("xl/worksheets/sheet2.xml"))
        assertFalse("Não deve incluir sheet3 quando cartões não forem selecionados", entries.containsKey("xl/worksheets/sheet3.xml"))

        val workbookXml = entries["xl/workbook.xml"]!!
        assertTrue(workbookXml.contains("Movimentações"))
        assertFalse(workbookXml.contains("Cartões"))
        assertFalse(workbookXml.contains("Faturas"))
    }

    @Test
    fun testExportTypeFilterExpensesOnly() {
        val out = ByteArrayOutputStream()
        SpreadsheetExporter.export(
            outputStream = out,
            options = SpreadsheetExportOptions(
                typeScope = ExportTypeScope.EXPENSE_ONLY,
                includeCardsAndInvoices = false
            ),
            allTransactions = sampleTransactions,
            filteredTransactions = sampleTransactions,
            cards = emptyList(),
            payments = emptyList()
        )

        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entries[entry.name] = zis.readBytes().toString(Charsets.UTF_8)
                entry = zis.nextEntry
            }
        }

        val sheet1Xml = entries["xl/worksheets/sheet1.xml"]!!
        assertTrue(sheet1Xml.contains("Supermercado"))
        assertTrue(sheet1Xml.contains("Restaurante Italiano"))
        assertFalse("Não deve conter a receita de salário quando filtrado por despesas", sheet1Xml.contains("Salário Empresa"))
    }

    @Test
    fun testExcelSerialDateConversion() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 22, 0, 0, 0)
        }
        val serialDate = SpreadsheetExporter.millisToExcelDate(cal.timeInMillis)
        // 2026-09-22 em número serial do Excel fica em torno de 46287
        assertTrue("Data serial do Excel para 2026 deve ser > 45000", serialDate > 45000)
    }

    @Test
    fun testColumnLetterConversion() {
        assertEquals("A", SpreadsheetExporter.toColumnLetter(0))
        assertEquals("B", SpreadsheetExporter.toColumnLetter(1))
        assertEquals("Z", SpreadsheetExporter.toColumnLetter(25))
        assertEquals("AA", SpreadsheetExporter.toColumnLetter(26))
        assertEquals("AB", SpreadsheetExporter.toColumnLetter(27))
    }
}
