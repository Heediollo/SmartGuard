package com.example.smartguard.call

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.smartguard.MainActivity
import com.example.smartguard.R
import com.example.smartguard.database.BlockedCallHistory
import com.example.smartguard.database.BlockedNumbersDatabase
import com.example.smartguard.security.SecurityAudit
import kotlinx.coroutines.*

class BlockedCallScreeningService : CallScreeningService() {

    companion object {
        private const val TAG = "BlockedCallScreening"
        private const val CHANNEL_ID = "blocked_calls_channel"
        private const val NOTIFICATION_ID = 6001
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Выведем все номера из базы для диагностики
        serviceScope.launch {
            try {
                val db = BlockedNumbersDatabase.getDatabase(this@BlockedCallScreeningService)
                val numbers = db.blockedNumberDao().getAllBlockedNumbersOnce()
                Log.d(TAG, "=== Номера в чёрном списке (всего ${numbers.size}) ===")
                numbers.forEach { Log.d(TAG, "   ${it.phoneNumber}") }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка чтения базы: ${e.message}")
            }
        }
        Log.d(TAG, "✅ Service created")
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val rawNumber = callDetails.handle?.schemeSpecificPart ?: run {
            // Нет номера — разрешаем звонок
            respondToCall(callDetails, CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .build())
            return
        }
        val phoneNumber = normalizePhoneNumber(rawNumber)

        Log.d(TAG, "📞 Screening call from: $rawNumber → $phoneNumber")

        // Проверяем блокировку СИНХРОННО
        val isBlocked = runBlocking {
            try {
                val db = BlockedNumbersDatabase.getDatabase(this@BlockedCallScreeningService)
                val result = db.blockedNumberDao().isNumberBlocked(phoneNumber)
                Log.d(TAG, "🔍 DB check for '$phoneNumber': $result")
                result
            } catch (e: Exception) {
                Log.e(TAG, "❌ DB error: ${e.message}")
                false
            }
        }

        if (isBlocked) {
            Log.w(TAG, "🚫 Blocking call from: $phoneNumber")

            // Отклоняем звонок
            respondToCall(callDetails, CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build())

            // Уведомление, аудит, история
            serviceScope.launch {
                showBlockedNotification(phoneNumber)

                SecurityAudit.logEvent(
                    this@BlockedCallScreeningService,
                    SecurityAudit.EventType.CALL_BLOCKED,
                    "Заблокирован входящий звонок: $phoneNumber",
                    SecurityAudit.RiskLevel.HIGH
                )

                try {
                    val db = BlockedNumbersDatabase.getDatabase(this@BlockedCallScreeningService)
                    db.blockedCallHistoryDao().insert(BlockedCallHistory(phoneNumber = phoneNumber))
                    Log.d(TAG, "✅ History saved")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to save history: ${e.message}")
                }
            }
        } else {
            Log.d(TAG, "✅ Call allowed: $phoneNumber")
            respondToCall(callDetails, CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .build())
        }
    }

    // Унифицированная нормализация (как в RealTimeCallService)
    private fun normalizePhoneNumber(number: String): String {
        val cleaned = number.replace("[^+0-9]".toRegex(), "")
        return when {
            cleaned.startsWith("+") -> cleaned
            cleaned.startsWith("8") && cleaned.length == 11 -> "+7" + cleaned.substring(1)
            cleaned.length == 11 && (cleaned.startsWith("7") || cleaned.startsWith("8")) -> "+7" + cleaned.substring(1)
            cleaned.length == 10 -> "+7" + cleaned
            else -> "+$cleaned"
        }
    }

    private fun showBlockedNotification(phoneNumber: String) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚫 Заблокирован звонок")
            .setContentText("Мошеннический номер: $phoneNumber")
            .setSmallIcon(R.drawable.ic_shield)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Заблокированные звонки",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о блокировке мошеннических звонков"
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
}