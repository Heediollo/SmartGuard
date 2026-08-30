package com.example.smartguard.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.smartguard.security.SecureStorage
import com.example.smartguard.security.SecurityAudit
import com.example.smartguard.utils.AIAnalyzer
import com.example.smartguard.utils.NotificationHelper
import com.example.smartguard.utils.SmsAnalyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        // Проверяем, включена ли SMS-защита
        val prefs = SecureStorage.getEncryptedPrefs(context)
        val smsProtectionEnabled = prefs.getBoolean("sms_protection_enabled", true)
        if (!smsProtectionEnabled) {
            Log.d("SmsReceiver", "SMS-защита отключена, сообщение пропущено")
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val bundle = intent.extras ?: return
        val pdus = bundle["pdus"] as? Array<*> ?: return

        for (pdu in pdus) {
            val message = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val format = intent.getStringExtra("format") ?: "3gpp"
                SmsMessage.createFromPdu(pdu as ByteArray, format)
            } else {
                @Suppress("DEPRECATION")
                SmsMessage.createFromPdu(pdu as ByteArray)
            }

            val sender = message.displayOriginatingAddress ?: "Неизвестен"
            val body = message.messageBody ?: ""
            val timestamp = message.timestampMillis

            Log.d("SmsReceiver", "Получено SMS от $sender: $body")

            analyzeSmsHybrid(context.applicationContext, sender, body, timestamp)
        }
    }

    private fun analyzeSmsHybrid(context: Context, sender: String, body: String, timestamp: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val rulesResult = SmsAnalyzer.analyze(body)

                if (rulesResult.confidence >= 80) {
                    handlePhishingDetected(context, sender, body, rulesResult.category, rulesResult.confidence, "Правила", timestamp)
                } else if (rulesResult.confidence >= 40) {
                    val aiResult = AIAnalyzer.analyzeWithAI(body)
                    val finalConfidence = maxOf(rulesResult.confidence, aiResult.confidence)
                    val finalCategory = if (aiResult.confidence > rulesResult.confidence) aiResult.category else rulesResult.category

                    if (finalConfidence >= 50) {
                        handlePhishingDetected(context, sender, body, finalCategory, finalConfidence, "Гибрид", timestamp)
                    }
                }
            } catch (e: Exception) {
                Log.e("SmsReceiver", "Ошибка анализа: ${e.message}")
                SecurityAudit.logEvent(
                    context,
                    SecurityAudit.EventType.ERROR,
                    "Ошибка анализа SMS: ${e.message}",
                    SecurityAudit.RiskLevel.MEDIUM,
                    sender
                )
            }
        }
    }

    private suspend fun handlePhishingDetected(
        context: Context,
        sender: String,
        body: String,
        category: String,
        confidence: Int,
        method: String,
        timestamp: Long
    ) {
        withContext(Dispatchers.Main) {
            NotificationHelper.showPhishingAlert(context, sender, body, category, confidence)

            SecurityAudit.logEvent(
                context,
                SecurityAudit.EventType.SMS_PHISHING_DETECTED,
                "Фишинг ($method): $category, уверенность: $confidence%",
                SecurityAudit.RiskLevel.HIGH,
                sender
            )

            Log.d("SmsReceiver", "🚨 ФИШИНГ ОБНАРУЖЕН: $category ($confidence%)")
        }

        // Удаляем фишинговое SMS
        deleteSms(context, sender, body)
    }

    private fun deleteSms(context: Context, sender: String, body: String) {
        try {
            val uri = Telephony.Sms.Inbox.CONTENT_URI
            val selection = "address = ? AND body = ?"
            val selectionArgs = arrayOf(sender, body)

            // Помечаем как прочитанное
            val values = ContentValues().apply {
                put("read", 1)
                put("seen", 1)
            }
            val updated = context.contentResolver.update(uri, values, selection, selectionArgs)
            Log.d("SmsReceiver", "Помечено как прочитанное: $updated записей")

            // Удаляем
            val deleted = context.contentResolver.delete(uri, selection, selectionArgs)
            if (deleted > 0) {
                Log.d("SmsReceiver", "✅ Фишинговое SMS от $sender удалено")
            } else {
                Log.w("SmsReceiver", "⚠️ Не удалось найти SMS для удаления")
            }
        } catch (e: Exception) {
            Log.e("SmsReceiver", "❌ Ошибка удаления SMS: ${e.message}")
        }
    }
}