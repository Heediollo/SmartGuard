package com.example.smartguard.utils

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    private const val CHANNEL_ID = "phishing_alerts"
    private const val CHANNEL_NAME = "Предупреждения о фишинге"
    private const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Критические предупреждения о фишинге"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showPhishingAlert(
        context: Context,
        sender: String,
        message: String,
        category: String,
        confidence: Int
    ) {
        createNotificationChannel(context)

        // Intent без явной ссылки на MainActivity (чтобы избежать ошибок)
        val packageName = context.packageName
        val intent = Intent().apply {
            setClassName(packageName, "$packageName.MainActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val categoryText = getCategoryDescription(category)
        val truncatedMessage = if (message.length > 100) message.take(100) + "..." else message

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dialog_alert)
            .setContentTitle("🚨 ОБНАРУЖЕН ФИШИНГ!")
            .setContentText("$categoryText\nОт: $sender")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "$categoryText\n\n" +
                                "📞 От: $sender\n\n" +
                                "📨 Сообщение:\n$truncatedMessage\n\n" +
                                "⚠️ Уровень угрозы: ${confidence}%"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun getCategoryDescription(category: String): String {
        return when (category) {
            "bank_scam" -> "🏦 Банковское мошенничество"
            "government_scam" -> "🏛️ Фейковые госорганы"
            "tech_support" -> "💻 Фейковая техподдержка"
            "prize_scam" -> "🎁 Мошенничество с призами"
            "social_engineering" -> "🎭 Социальная инженерия"
            "delivery_scam" -> "📦 Фейковая доставка"
            "phone_scam" -> "📞 Телефонное мошенничество"
            else -> "⚠️ Подозрительное сообщение"
        }
    }
}