package com.example.smartguard.call

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Управление уведомлениями для фонового сервиса записи
 */
object CallRecordingNotification {

    private const val CHANNEL_ID = "smartguard_call_recording_channel"
    private const val NOTIFICATION_ID = 2001

    /**
     * Создает канал уведомлений (нужно только один раз)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SmartGuard: Запись звонков",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Показывает когда запись звонков активна"
                enableLights(false)
                enableVibration(false)
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)

            android.util.Log.d("Notification", "✅ Канал уведомлений создан")
        }
    }

    /**
     * Показывает уведомление о активной записи
     * @return ID уведомления
     */
    fun showRecordingNotification(context: Context): Int {
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        // Intent при клике на уведомление
        val intent = Intent(context, Class.forName("com.example.smartguard.MainActivity"))
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Строим уведомление
        val builder = android.app.Notification.Builder(context, CHANNEL_ID)
            .setContentTitle("🛡️ SmartGuard")
            .setContentText("Идёт анализ звонка...")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(android.app.Notification.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Невозможно удалить без остановки сервиса
            .setAutoCancel(false)

        val notification = builder.build()
        notificationManager.notify(NOTIFICATION_ID, notification)

        android.util.Log.d("Notification", "✅ Уведомление о записи показано")

        return NOTIFICATION_ID
    }

    /**
     * Убирает уведомление
     */
    fun cancelNotification(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancel(NOTIFICATION_ID)
        android.util.Log.d("Notification", "❌ Уведомление удалено")
    }
}