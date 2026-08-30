package com.example.smartguard.database

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Утилита для импорта номеров из JSON-файла в локальную базу данных.
 */
object BlockedNumbersImporter {

    private const val TAG = "BlockedNumbersImporter"
    private const val JSON_FILE_NAME = "blocked_numbers.json"

    /**
     * Импортирует номера из assets/blocked_numbers.json в базу данных.
     * Если номера уже есть в базе, они не дублируются.
     */
    fun importFromAssets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Читаем JSON-строку из файла в assets
                val jsonString = context.assets.open(JSON_FILE_NAME)
                    .bufferedReader()
                    .use { it.readText() }

                // 2. Парсим JSON в список объектов ImportedNumber
                val type = object : TypeToken<List<ImportedNumber>>() {}.type
                val importedNumbers: List<ImportedNumber> = Gson().fromJson(jsonString, type)

                Log.d(TAG, "Найдено ${importedNumbers.size} номеров в JSON")

                // 3. Получаем базу данных
                val db = BlockedNumbersDatabase.getDatabase(context)
                val dao = db.blockedNumberDao()

                // 4. Вставляем каждый номер, если его ещё нет
                var insertedCount = 0
                importedNumbers.forEach { imported ->
                    val exists = dao.findByNumber(imported.phoneNumber) == null
                    if (exists) {
                        dao.insert(
                            BlockedNumber(
                                phoneNumber = imported.phoneNumber,
                                category = imported.category,
                                dateAdded = System.currentTimeMillis()
                            )
                        )
                        insertedCount++
                    }
                }

                Log.d(TAG, "Успешно импортировано $insertedCount новых номеров")

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка импорта номеров: ${e.message}", e)
            }
        }
    }

    // Вспомогательный класс для парсинга JSON
    private data class ImportedNumber(
        val phoneNumber: String,
        val category: String
    )
}