package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, id DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE dateMillis >= :startMillis AND dateMillis <= :endMillis ORDER BY dateMillis DESC, id DESC")
    fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionByIdOnce(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY dateMillis DESC")
    fun getTransactionsByCategory(categoryId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE cardId = :cardId ORDER BY dateMillis DESC")
    fun getTransactionsByCard(cardId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE cardId = :cardId AND invoiceMonthYear = :invoiceMonthYear ORDER BY dateMillis ASC")
    fun getTransactionsByCardAndInvoice(cardId: Long, invoiceMonthYear: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE installmentGroupId = :groupId ORDER BY installmentNumber ASC")
    fun getTransactionsByInstallmentGroup(groupId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE installmentGroupId = :groupId ORDER BY installmentNumber ASC")
    suspend fun getTransactionsByInstallmentGroupSync(groupId: String): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions WHERE installmentGroupId = :groupId")
    suspend fun deleteTransactionsByInstallmentGroup(groupId: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}
