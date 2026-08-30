package com.example.smartguard.utils

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import android.telephony.PhoneNumberUtils

/**
 * 🔍 Helper для проверки: известен ли номер (есть в контактах)
 */
object ContactsHelper {
    private const val TAG = "ContactsHelper"

    /**
     * Проверяет, есть ли номер в адресной книге пользователя
     * @param phoneNumber номер в любом формате (+7..., 8..., 7...)
     * @param context контекст приложения
     * @return true если номер найден в контактах
     */
    fun isKnownNumber(phoneNumber: String, context: Context): Boolean {
        if (phoneNumber.isBlank() || phoneNumber == "Неизвестный номер") {
            return false
        }

        return try {
            // Нормализуем номер для сравнения (убираем пробелы, тире, скобки)
            val normalizedInput = normalizePhoneNumber(phoneNumber)

            // URI для поиска по номеру телефона
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )

            // Запрашиваем контакт
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup._ID), // нам нужен только ID
                null,
                null,
                null
            )

            val isKnown = cursor?.use {
                it.moveToFirst() // если есть хотя бы одна запись — номер известен
            } == true

            if (isKnown) {
                Log.d(TAG, "✅ Номер найден в контактах: $phoneNumber")
            } else {
                Log.d(TAG, "❌ Номер НЕ в контактах: $phoneNumber")
            }

            isKnown

        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка при проверке контакта: ${e.message}", e)
            false // При ошибке считаем номер неизвестным (безопаснее)
        }
    }

    /**
     * Нормализация номера: убирает лишние символы для надёжного сравнения
     * Пример: "+7 (999) 123-45-67" → "79991234567"
     */
    private fun normalizePhoneNumber(phone: String): String {
        return phone.replace(Regex("[^+\\d]"), "") // оставляем только цифры и +
            .let {
                // Если начинается с 8 — заменяем на +7 (для РФ/КЗ)
                if (it.startsWith("8") && it.length == 11) {
                    "+7${it.substring(1)}"
                } else {
                    it
                }
            }
    }

    /**
     * Альтернативная проверка: сравнение по очищенным цифрам
     * (если CONTENT_FILTER_URI не сработал)
     */
    fun isKnownNumberFallback(phoneNumber: String, context: Context): Boolean {
        return try {
            val normalizedInput = normalizePhoneNumber(phoneNumber)

            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                null,
                null,
                null
            )

            val isFound = cursor?.use {
                val numberIndex = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    val contactNumber = it.getString(numberIndex)
                    val normalizedContact = normalizePhoneNumber(contactNumber)

                    // Сравниваем последние 10 цифр (универсально для разных форматов)
                    if (normalizedInput.takeLast(10) == normalizedContact.takeLast(10)) {
                        return@use true
                    }
                }
                false
            } == true

            if (isFound) {
                Log.d(TAG, "✅ Номер найден через fallback: $phoneNumber")
            }
            isFound

        } catch (e: Exception) {
            Log.e(TAG, "❌ Fallback error: ${e.message}", e)
            false
        }
    }
}