package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconKey: String = "category",
    val colorHex: Long = 0xFF4CAF50,
    val type: TransactionType = TransactionType.EXPENSE,
    val isPredefined: Boolean = false
)
