package com.example.smartguard.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 🔐 Безопасное хранилище на основе EncryptedSharedPreferences.
 * Все данные автоматически шифруются алгоритмом AES-256.
 */
object SecureStorage {

    private const val PREFS_NAME = "smartguard_secure_prefs"

    /**
     * Получить зашифрованное SharedPreferences.
     */
    fun getEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Сохранить строку.
     */
    fun putString(context: Context, key: String, value: String) {
        getEncryptedPrefs(context).edit().putString(key, value).apply()
    }

    /**
     * Получить строку.
     */
    fun getString(context: Context, key: String, defaultValue: String = ""): String {
        return getEncryptedPrefs(context).getString(key, defaultValue) ?: defaultValue
    }

    /**
     * Сохранить булево значение.
     */
    fun putBoolean(context: Context, key: String, value: Boolean) {
        getEncryptedPrefs(context).edit().putBoolean(key, value).apply()
    }

    /**
     * Получить булево значение.
     */
    fun getBoolean(context: Context, key: String, defaultValue: Boolean = false): Boolean {
        return getEncryptedPrefs(context).getBoolean(key, defaultValue)
    }

    /**
     * Удалить значение по ключу.
     */
    fun remove(context: Context, key: String) {
        getEncryptedPrefs(context).edit().remove(key).apply()
    }

    /**
     * Очистить всё хранилище.
     */
    fun clearAll(context: Context) {
        getEncryptedPrefs(context).edit().clear().apply()
    }
}