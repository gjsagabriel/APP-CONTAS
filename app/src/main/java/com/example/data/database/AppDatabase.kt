package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.BudgetDao
import com.example.data.dao.CardInvoicePaymentDao
import com.example.data.dao.CategoryDao
import com.example.data.dao.CreditCardDao
import com.example.data.dao.TransactionDao
import com.example.data.model.BudgetEntity
import com.example.data.model.CardInvoicePaymentEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.CreditCardEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        CreditCardEntity::class,
        CardInvoicePaymentEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun cardInvoicePaymentDao(): CardInvoicePaymentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Alterar tabela de transações com as novas colunas
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `paymentMethod` TEXT NOT NULL DEFAULT 'UNSPECIFIED'")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `cardId` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `cardName` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `installmentNumber` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `totalInstallments` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `installmentGroupId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `invoiceMonthYear` TEXT NOT NULL DEFAULT ''")

                // 2. Criar tabela de cartões
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `credit_cards` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `issuer` TEXT NOT NULL,
                        `network` TEXT NOT NULL,
                        `colorHex` INTEGER NOT NULL,
                        `lastFourDigits` TEXT NOT NULL,
                        `creditLimitCents` INTEGER NOT NULL,
                        `closingDay` INTEGER NOT NULL,
                        `dueDay` INTEGER NOT NULL,
                        `isArchived` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                // 3. Criar tabela de pagamentos de faturas
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `card_invoice_payments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `cardId` INTEGER NOT NULL,
                        `invoiceMonthYear` TEXT NOT NULL,
                        `amountPaidCents` INTEGER NOT NULL,
                        `paymentDateMillis` INTEGER NOT NULL,
                        `isPaid` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                // 4. Criar índice único para pagamentos de fatura por cartão e mês/ano
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_card_invoice_payments_cardId_invoiceMonthYear` ON `card_invoice_payments` (`cardId`, `invoiceMonthYear`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finanzo_database.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getDatabase(context)
                                database.categoryDao().insertCategories(PredefinedData.defaultCategories)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
