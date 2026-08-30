package com.example.smartguard.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * База данных спам-номеров
 * Хранит локальную базу известных номеров мошенников
 */
class SpamDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "spam_numbers.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "spam_numbers"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NUMBER = "phone_number"
        private const val COLUMN_CATEGORY = "category"
        private const val COLUMN_DATE_ADDED = "date_added"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NUMBER TEXT UNIQUE NOT NULL,
                $COLUMN_CATEGORY TEXT DEFAULT 'unknown',
                $COLUMN_DATE_ADDED TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent()
        db?.execSQL(createTable)

        // Добавляем тестовые спам-номера для демонстрации
        addTestNumbers(db)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    private fun addTestNumbers(db: SQLiteDatabase?) {
        val testNumbers = listOf(
            "+78001234567" to "bank_scam",
            "+79999999999" to "tech_support",
            "+74951234567" to "government",
            "88001234567" to "bank_scam",
            "+78005553535" to "delivery",
            "+79991234567" to "unknown"
        )

        testNumbers.forEach { (number, category) ->
            val values = ContentValues().apply {
                put(COLUMN_NUMBER, number)
                put(COLUMN_CATEGORY, category)
            }
            db?.insert(TABLE_NAME, null, values)
        }
    }

    /**
     * Проверка: является ли номер спамом
     */
    fun isSpamNumber(phoneNumber: String): Boolean {
        val db = this.readableDatabase
        val cleanNumber = phoneNumber.replace("\\D".toRegex(), "")

        // Проверяем разные форматы номера
        val query = """
            SELECT * FROM $TABLE_NAME 
            WHERE replace($COLUMN_NUMBER, '+', '') LIKE ? 
            OR replace($COLUMN_NUMBER, '8', '') LIKE ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf("%$cleanNumber%", "%${cleanNumber.drop(1)}%"))
        val isSpam = cursor.count > 0
        cursor.close()
        db.close()

        return isSpam
    }

    /**
     * Получить категорию спама
     */
    fun getSpamCategory(phoneNumber: String): String {
        val db = this.readableDatabase
        val cleanNumber = phoneNumber.replace("\\D".toRegex(), "")

        val query = "SELECT $COLUMN_CATEGORY FROM $TABLE_NAME WHERE replace($COLUMN_NUMBER, '+', '') LIKE ?"
        val cursor = db.rawQuery(query, arrayOf("%$cleanNumber%"))

        var category = "unknown"
        if (cursor.moveToFirst()) {
            category = cursor.getString(0)
        }
        cursor.close()
        db.close()

        return category
    }

    /**
     * Добавить номер в базу спама
     */
    fun addSpamNumber(number: String, category: String = "user_reported") {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NUMBER, number)
            put(COLUMN_CATEGORY, category)
        }
        db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        db.close()
    }

    /**
     * Получить количество записей в базе
     */
    fun getSpamCount(): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_NAME", null)
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        db.close()
        return count
    }
}