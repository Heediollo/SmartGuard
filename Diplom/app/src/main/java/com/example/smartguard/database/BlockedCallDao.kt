package com.example.smartguard.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ЫBlockedCallDao {
    @Insert
    suspend fun insert(call: BlockedCallHistory)

    @Query("SELECT * FROM blocked_calls_history ORDER BY timestamp DESC")
    fun getAllBlockedCalls(): Flow<List<BlockedCallHistory>>
}