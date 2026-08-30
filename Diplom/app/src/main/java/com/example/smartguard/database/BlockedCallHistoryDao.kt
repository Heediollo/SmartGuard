package com.example.smartguard.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BlockedCallHistoryDao {
    @Insert
    suspend fun insert(history: BlockedCallHistory)

    @Query("SELECT * FROM blocked_call_history ORDER BY timestamp DESC")
    suspend fun getAllHistory(): List<BlockedCallHistory>

    @Query("DELETE FROM blocked_call_history")
    suspend fun clearAll()
}