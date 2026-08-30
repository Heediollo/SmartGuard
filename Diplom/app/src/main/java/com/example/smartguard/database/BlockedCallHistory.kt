package com.example.smartguard.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_call_history")
data class BlockedCallHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val timestamp: Long = System.currentTimeMillis()
)