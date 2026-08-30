package com.example.smartguard.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [BlockedNumber::class, BlockedCallHistory::class],
    version = 2,
    exportSchema = false
)
abstract class BlockedNumbersDatabase : RoomDatabase() {
    abstract fun blockedNumberDao(): BlockedNumberDao
    abstract fun blockedCallHistoryDao(): BlockedCallHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: BlockedNumbersDatabase? = null

        fun getDatabase(context: Context): BlockedNumbersDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BlockedNumbersDatabase::class.java,
                    "blocked_numbers_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Создаём таблицу для истории заблокированных звонков
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS blocked_call_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        phoneNumber TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """)
            }
        }
    }
}