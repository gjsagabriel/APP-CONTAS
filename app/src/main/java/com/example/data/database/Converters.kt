package com.example.data.database

import androidx.room.TypeConverter
import com.example.data.model.CardType
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String {
        return type.name
    }

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return try {
            TransactionType.valueOf(value)
        } catch (_: Exception) {
            TransactionType.EXPENSE
        }
    }

    @TypeConverter
    fun fromPaymentMethod(method: PaymentMethod): String {
        return method.name
    }

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod {
        return try {
            PaymentMethod.valueOf(value)
        } catch (_: Exception) {
            PaymentMethod.UNSPECIFIED
        }
    }

    @TypeConverter
    fun fromCardType(type: CardType): String {
        return type.name
    }

    @TypeConverter
    fun toCardType(value: String): CardType {
        return try {
            CardType.valueOf(value)
        } catch (_: Exception) {
            CardType.CREDIT
        }
    }
}
