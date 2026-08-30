package com.example.smartguard.call

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Менеджер разрешений для работы с звонками и записью аудио
 */
class CallPermissionManager(private val activity: Activity) {

    companion object {
        private const val CALL_PERMISSION_REQUEST_CODE = 2001

        // Список необходимых разрешений
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.READ_PHONE_STATE,           // Чтение состояния телефона
            Manifest.permission.RECORD_AUDIO,               // Запись звука микрофона
            Manifest.permission.PROCESS_OUTGOING_CALLS,     // Обработка исходящих вызовов (Deprecated в Android 10+, но нужен для старых версий)
            Manifest.permission.ANSWER_PHONE_CALLS,         // Ответ на входящие вызовы
            Manifest.permission.CALL_PHONE                  // Совершение звонков
        )
    }

    /**
     * Проверяет есть ли все необходимые разрешения
     */
    fun checkPermissions(): Boolean {
        var allGranted = true
        REQUIRED_PERMISSIONS.forEach { permission ->
            if (ContextCompat.checkSelfPermission(activity, permission)
                != PackageManager.PERMISSION_GRANTED) {
                allGranted = false
            }
        }
        return allGranted
    }

    /**
     * Запрашивает разрешения у пользователя
     */
    fun requestPermissions() {
        ActivityCompat.requestPermissions(
            activity,
            REQUIRED_PERMISSIONS,
            CALL_PERMISSION_REQUEST_CODE
        )

        android.widget.Toast.makeText(
            activity,
            "Пожалуйста предоставьте все разрешения для работы звонков",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    /**
     * Получает список неисполненных разрешений
     */
    fun getMissingPermissions(): List<String> {
        val missing = mutableListOf<String>()
        REQUIRED_PERMISSIONS.forEach { permission ->
            if (ContextCompat.checkSelfPermission(activity, permission)
                != PackageManager.PERMISSION_GRANTED) {
                missing.add(permission)
            }
        }
        return missing
    }
}