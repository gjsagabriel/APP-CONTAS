package com.example.data.database

import com.example.data.model.BudgetEntity
import com.example.data.model.CardType
import com.example.data.model.CategoryEntity
import com.example.data.model.CreditCardEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.domain.util.DateUtils
import java.util.Calendar

object PredefinedData {

    val defaultCategories: List<CategoryEntity> = listOf(
        // Despesas
        CategoryEntity(
            id = 1,
            name = "Alimentação",
            iconKey = "restaurant",
            colorHex = 0xFFFF7043,
            type = TransactionType.EXPENSE,
            isPredefined = true
        ),
        CategoryEntity(
            id = 2,
            name = "Moradia",
            iconKey = "home",
            colorHex = 0xFF42A5F5,
            type = TransactionType.EXPENSE,
            isPredefined = true
        ),
        CategoryEntity(
            id = 3,
            name = "Transporte",
            iconKey = "directions_car",
            colorHex = 0xFFFFA726,
            type = TransactionType.EXPENSE,
            isPredefined = true
        ),
        CategoryEntity(
            id = 4,
            name = "Saúde",
            iconKey = "medical_services",
            colorHex = 0xFFEF5350,
            type = TransactionType.EXPENSE,
            isPredefined = true
        ),
        CategoryEntity(
            id = 5,
            name = "Educação",
            iconKey = "school",
            colorHex = 0xFFAB47BC,
            type = TransactionType.EXPENSE,
            isPredefined = true
        ),
        CategoryEntity(
            id = 6,
            name = "Lazer",
            iconKey = "movie",
            colorHex = 0xFF26A69A,
            type = TransactionType.EXPENSE,
            isPredefined = true
        ),
        CategoryEntity(
            id = 7,
            name = "Compras",
            iconKey = "shopping_cart",
            colorHex = 0xFFEC407A,
            type = TransactionType.EXPENSE,
            isPredefined = true
        ),
        CategoryEntity(
            id = 8,
            name = "Outras Despesas",
            iconKey = "more_horiz",
            colorHex = 0xFF78909C,
            type = TransactionType.EXPENSE,
            isPredefined = true
        ),

        // Receitas
        CategoryEntity(
            id = 9,
            name = "Salário",
            iconKey = "payments",
            colorHex = 0xFF2E7D32,
            type = TransactionType.INCOME,
            isPredefined = true
        ),
        CategoryEntity(
            id = 10,
            name = "Rendimentos",
            iconKey = "trending_up",
            colorHex = 0xFF00897B,
            type = TransactionType.INCOME,
            isPredefined = true
        ),
        CategoryEntity(
            id = 11,
            name = "Freelance",
            iconKey = "work",
            colorHex = 0xFF039BE5,
            type = TransactionType.INCOME,
            isPredefined = true
        ),
        CategoryEntity(
            id = 12,
            name = "Presentes",
            iconKey = "redeem",
            colorHex = 0xFF8E24AA,
            type = TransactionType.INCOME,
            isPredefined = true
        ),
        CategoryEntity(
            id = 13,
            name = "Outras Receitas",
            iconKey = "account_balance_wallet",
            colorHex = 0xFF43A047,
            type = TransactionType.INCOME,
            isPredefined = true
        )
    )

    fun createDemoCards(): List<CreditCardEntity> {
        return listOf(
            CreditCardEntity(
                id = 1,
                name = "Nubank Roxinho",
                type = CardType.MULTIPLE,
                issuer = "Nubank",
                network = "Mastercard",
                colorHex = 0xFF8A05BE,
                lastFourDigits = "8921",
                creditLimitCents = 500000L, // R$ 5.000,00
                closingDay = 20,
                dueDay = 27
            ),
            CreditCardEntity(
                id = 2,
                name = "Inter Black",
                type = CardType.CREDIT,
                issuer = "Banco Inter",
                network = "Mastercard",
                colorHex = 0xFFFF7A00,
                lastFourDigits = "4310",
                creditLimitCents = 1200000L, // R$ 12.000,00
                closingDay = 5,
                dueDay = 15
            ),
            CreditCardEntity(
                id = 3,
                name = "Itaú Débito",
                type = CardType.DEBIT,
                issuer = "Itaú",
                network = "Visa",
                colorHex = 0xFFEC7000,
                lastFourDigits = "1098",
                creditLimitCents = 0L,
                closingDay = 1,
                dueDay = 10
            )
        )
    }

    fun createDemoTransactions(): List<TransactionEntity> {
        fun daysAgo(days: Int): Long {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -days)
            return cal.timeInMillis
        }

        val currentMonthYear = DateUtils.getCurrentMonthYear()
        val nextMonthYear = DateUtils.getNextMonthYear(currentMonthYear)

        return listOf(
            TransactionEntity(
                description = "Salário Mensal",
                amountCents = 550000L, // R$ 5.500,00
                type = TransactionType.INCOME,
                categoryId = 9,
                categoryName = "Salário",
                dateMillis = daysAgo(15),
                paymentMethod = PaymentMethod.PIX,
                note = "Depósito CLT"
            ),
            TransactionEntity(
                description = "Projeto Freelance Design",
                amountCents = 120000L, // R$ 1.200,00
                type = TransactionType.INCOME,
                categoryId = 11,
                categoryName = "Freelance",
                dateMillis = daysAgo(6),
                paymentMethod = PaymentMethod.PIX,
                note = "Landing page para cliente"
            ),
            TransactionEntity(
                description = "Dividendos e Fundos",
                amountCents = 18550L, // R$ 185,50
                type = TransactionType.INCOME,
                categoryId = 10,
                categoryName = "Rendimentos",
                dateMillis = daysAgo(10),
                paymentMethod = PaymentMethod.WALLET,
                note = "Rendimentos FIIs"
            ),
            TransactionEntity(
                description = "Aluguel e Condomínio",
                amountCents = 175000L, // R$ 1.750,00
                type = TransactionType.EXPENSE,
                categoryId = 2,
                categoryName = "Moradia",
                dateMillis = daysAgo(12),
                paymentMethod = PaymentMethod.PIX,
                note = "Vencimento dia 10"
            ),
            TransactionEntity(
                description = "Supermercado Mensal",
                amountCents = 84230L, // R$ 842,30
                type = TransactionType.EXPENSE,
                categoryId = 1,
                categoryName = "Alimentação",
                dateMillis = daysAgo(8),
                paymentMethod = PaymentMethod.CREDIT_CARD,
                cardId = 1,
                cardName = "Nubank Roxinho",
                invoiceMonthYear = currentMonthYear,
                note = "Compras do mês"
            ),
            TransactionEntity(
                description = "Abastecimento Carro",
                amountCents = 21000L, // R$ 210,00
                type = TransactionType.EXPENSE,
                categoryId = 3,
                categoryName = "Transporte",
                dateMillis = daysAgo(4),
                paymentMethod = PaymentMethod.DEBIT_CARD,
                cardId = 3,
                cardName = "Itaú Débito",
                note = "Gasolina aditivada"
            ),
            TransactionEntity(
                description = "Farmácia e Vitaminas",
                amountCents = 11590L, // R$ 115,90
                type = TransactionType.EXPENSE,
                categoryId = 4,
                categoryName = "Saúde",
                dateMillis = daysAgo(3),
                paymentMethod = PaymentMethod.CREDIT_CARD,
                cardId = 1,
                cardName = "Nubank Roxinho",
                invoiceMonthYear = currentMonthYear,
                note = "Medicamentos"
            ),
            TransactionEntity(
                description = "Jantar com Amigos",
                amountCents = 14500L, // R$ 145,00
                type = TransactionType.EXPENSE,
                categoryId = 6,
                categoryName = "Lazer",
                dateMillis = daysAgo(2),
                paymentMethod = PaymentMethod.CREDIT_CARD,
                cardId = 2,
                cardName = "Inter Black",
                invoiceMonthYear = currentMonthYear,
                note = "Pizzaria"
            ),
            // Compra parcelada: Smartphone (1/2 e 2/2)
            TransactionEntity(
                description = "Smartphone (1/2)",
                amountCents = 75000L, // R$ 750,00
                type = TransactionType.EXPENSE,
                categoryId = 7,
                categoryName = "Compras",
                dateMillis = daysAgo(5),
                paymentMethod = PaymentMethod.CREDIT_CARD,
                cardId = 1,
                cardName = "Nubank Roxinho",
                installmentNumber = 1,
                totalInstallments = 2,
                installmentGroupId = "demo-smartphone-group",
                invoiceMonthYear = currentMonthYear,
                note = "Parcela 1 de 2"
            ),
            TransactionEntity(
                description = "Smartphone (2/2)",
                amountCents = 75000L, // R$ 750,00
                type = TransactionType.EXPENSE,
                categoryId = 7,
                categoryName = "Compras",
                dateMillis = daysAgo(5) + (30L * 24L * 3600L * 1000L),
                paymentMethod = PaymentMethod.CREDIT_CARD,
                cardId = 1,
                cardName = "Nubank Roxinho",
                installmentNumber = 2,
                totalInstallments = 2,
                installmentGroupId = "demo-smartphone-group",
                invoiceMonthYear = nextMonthYear,
                note = "Parcela 2 de 2"
            ),
            TransactionEntity(
                description = "Padaria do Bairro",
                amountCents = 3280L, // R$ 32,80
                type = TransactionType.EXPENSE,
                categoryId = 1,
                categoryName = "Alimentação",
                dateMillis = System.currentTimeMillis(),
                paymentMethod = PaymentMethod.CASH,
                note = "Café da manhã"
            )
        )
    }

    fun createDemoBudgets(monthYear: String = DateUtils.getCurrentMonthYear()): List<BudgetEntity> {
        return listOf(
            BudgetEntity(categoryId = 1, monthYear = monthYear, limitCents = 120000L), // R$ 1.200,00 Alimentação
            BudgetEntity(categoryId = 2, monthYear = monthYear, limitCents = 180000L), // R$ 1.800,00 Moradia
            BudgetEntity(categoryId = 3, monthYear = monthYear, limitCents = 40000L),  // R$ 400,00 Transporte
            BudgetEntity(categoryId = 4, monthYear = monthYear, limitCents = 30000L),  // R$ 300,00 Saúde
            BudgetEntity(categoryId = 5, monthYear = monthYear, limitCents = 35000L),  // R$ 350,00 Educação
            BudgetEntity(categoryId = 6, monthYear = monthYear, limitCents = 30000L)   // R$ 300,00 Lazer
        )
    }
}
