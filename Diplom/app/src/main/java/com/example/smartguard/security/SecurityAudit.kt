package com.example.smartguard.security

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

/**
 * Событие безопасности
 */
data class SecurityEvent(
    val id: Long,
    val timestamp: String,
    val eventType: String,
    val description: String,
    val riskLevel: String, // LOW, MEDIUM, HIGH, CRITICAL
    val phoneNumber: String = "",
    val details: String = ""
)

/**
 * Система аудита безопасности
 * Записывает все важные события в защищённый журнал
 */
object SecurityAudit {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val events = mutableListOf<SecurityEvent>()
    private var eventIdCounter = 0L

    /**
     * Типы событий безопасности
     */
    object EventType {
        const val PROTECTION_ENABLED = "PROTECTION_ENABLED"
        const val PROTECTION_DISABLED = "PROTECTION_DISABLED"
        const val SMS_PHISHING_DETECTED = "SMS_PHISHING_DETECTED"
        const val SPAM_CALL_DETECTED = "SPAM_CALL_DETECTED"
        const val AI_THREAT_DETECTED = "AI_THREAT_DETECTED"
        const val CALL_BLOCKED = "CALL_BLOCKED"
        const val PERMISSION_GRANTED = "PERMISSION_GRANTED"
        const val PERMISSION_DENIED = "PERMISSION_DENIED"
        const val ROOT_DETECTED = "ROOT_DETECTED"
        const val TAMPER_DETECTED = "TAMPER_DETECTED"       // 🔥 NEW: для Anti-Tampering
        const val DEBUGGER_DETECTED = "DEBUGGER_DETECTED"
        const val DATA_ENCRYPTED = "DATA_ENCRYPTED"
        const val USER_ACTION = "USER_ACTION"
        const val ERROR = "ERROR"
    }

    /**
     * Уровни риска
     */
    object RiskLevel {
        const val LOW = "LOW"
        const val MEDIUM = "MEDIUM"
        const val HIGH = "HIGH"
        const val CRITICAL = "CRITICAL"
    }

    /**
     * Запись события
     */
    fun logEvent(
        context: Context,
        eventType: String,
        description: String,
        riskLevel: String,
        phoneNumber: String = "",
        details: String = ""
    ) {
        val event = SecurityEvent(
            id = ++eventIdCounter,
            timestamp = dateFormat.format(Date()),
            eventType = eventType,
            description = description,
            riskLevel = riskLevel,
            phoneNumber = phoneNumber,
            details = details
        )

        events.add(event)

        // Сохраняем в зашифрованное хранилище
        saveEventToStorage(context, event)

        // Для отладки выводим в лог
        Log.d("SecurityAudit", "[$riskLevel] $eventType: $description")
    }

    /**
     * Сохранение события в хранилище
     */
    private fun saveEventToStorage(context: Context, event: SecurityEvent) {
        val prefs = SecureStorage.getEncryptedPrefs(context)
        val eventKey = "event_${event.id}"
        val eventData = "${event.timestamp}|${event.eventType}|${event.description}|${event.riskLevel}|${event.phoneNumber}"
        prefs.edit().putString(eventKey, eventData).apply()
    }

    /**
     * Получение всех событий
     */
    fun getAllEvents(): List<SecurityEvent> {
        return events.sortedByDescending { it.timestamp }
    }

    /**
     * Получение событий высокого риска
     */
    fun getHighRiskEvents(): List<SecurityEvent> {
        return events.filter { it.riskLevel == RiskLevel.HIGH || it.riskLevel == RiskLevel.CRITICAL }
    }

    /**
     * Получение статистики
     */
    fun getStatistics(): Map<String, Int> {
        return events.groupingBy { it.eventType }.eachCount()
    }

    /**
     * Очистка журнала
     */
    fun clearEvents(context: Context) {
        events.clear()
        SecureStorage.clearAll(context)
    }

    /**
     * Экспорт журнала (для отчёта)
     */
    fun exportToText(): String {
        val sb = StringBuilder()
        sb.append("=== SMARTGUARD SECURITY AUDIT LOG ===\n")
        sb.append("Generated: ${dateFormat.format(Date())}\n")
        sb.append("Total Events: ${events.size}\n\n")

        events.forEach { event ->
            sb.append("[${event.timestamp}] [${event.riskLevel}] ${event.eventType}\n")
            sb.append("  Description: ${event.description}\n")
            if (event.phoneNumber.isNotEmpty()) {
                sb.append("  Phone: ${event.phoneNumber}\n")
            }
            sb.append("\n")
        }

        return sb.toString()
    }
}