package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CardInvoicePaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardInvoicePaymentDao {

    @Query("SELECT * FROM card_invoice_payments")
    fun getAllPayments(): Flow<List<CardInvoicePaymentEntity>>

    @Query("SELECT * FROM card_invoice_payments WHERE cardId = :cardId")
    fun getPaymentsForCard(cardId: Long): Flow<List<CardInvoicePaymentEntity>>

    @Query("SELECT * FROM card_invoice_payments WHERE cardId = :cardId AND invoiceMonthYear = :invoiceMonthYear LIMIT 1")
    fun getPayment(cardId: Long, invoiceMonthYear: String): Flow<CardInvoicePaymentEntity?>

    @Query("SELECT * FROM card_invoice_payments WHERE cardId = :cardId AND invoiceMonthYear = :invoiceMonthYear LIMIT 1")
    suspend fun getPaymentSync(cardId: Long, invoiceMonthYear: String): CardInvoicePaymentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: CardInvoicePaymentEntity): Long

    @Query("DELETE FROM card_invoice_payments WHERE cardId = :cardId AND invoiceMonthYear = :invoiceMonthYear")
    suspend fun deletePayment(cardId: Long, invoiceMonthYear: String)

    @Query("DELETE FROM card_invoice_payments WHERE cardId = :cardId")
    suspend fun deletePaymentsForCard(cardId: Long)

    @Query("DELETE FROM card_invoice_payments")
    suspend fun deleteAllPayments()
}
