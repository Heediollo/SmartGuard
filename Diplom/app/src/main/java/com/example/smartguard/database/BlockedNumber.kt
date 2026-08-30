package com.example.smartguard.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_numbers")
data class BlockedNumber(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,            // номер в формате +7...
    val category: String = "spam",      // "spam", "fraud", "collector"
    val dateAdded: Long = System.currentTimeMillis()
)