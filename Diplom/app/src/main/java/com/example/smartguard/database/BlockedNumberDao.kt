package com.example.smartguard.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedNumberDao {
    @Insert
    suspend fun insert(number: BlockedNumber)

    @Delete
    suspend fun delete(number: BlockedNumber)

    @Query("SELECT * FROM blocked_numbers WHERE phoneNumber = :number")
    suspend fun findByNumber(number: String): BlockedNumber?

    @Query("SELECT * FROM blocked_numbers ORDER BY dateAdded DESC")
    fun getAllNumbers(): Flow<List<BlockedNumber>>

    // 🔥 НОВЫЙ МЕТОД: получить все номера один раз (для статистики)
    @Query("SELECT * FROM blocked_numbers")
    suspend fun getAllBlockedNumbersOnce(): List<BlockedNumber>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_numbers WHERE phoneNumber = :number)")
    suspend fun isNumberBlocked(number: String): Boolean
}