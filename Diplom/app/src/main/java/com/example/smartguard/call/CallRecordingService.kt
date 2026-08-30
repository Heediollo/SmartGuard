package com.example.smartguard.call

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.util.Log

/**
 * Foreground Service для записи звонков в фоне
 */
class CallRecordingService : Service() {

    private var audioRecorder: AudioRecorderService? = null
    private var notificationId = 2001

    companion object {
        // ✅ ИСПРАВЛЕНО: Убрано слово private, теперь доступно другим классам
        const val ACTION_START_RECORDING = "ACTION_START_RECORDING"
        const val ACTION_STOP_RECORDING = "ACTION_STOP_RECORDING"
        const val CHANNEL_ID = "smartguard_call_service_channel"

        // Статус текущего состояния записи
        var isRecording = false
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("CallRecordingService", "✅ Сервис создан")

        // Создаём канал уведомлений
        createNotificationChannel()
        CallRecordingNotification.createNotificationChannel(this)

        // Создаём сервис записи аудио
        audioRecorder = AudioRecorderService(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("CallRecordingService", "⚙️ Сервис запущен")

        when (intent?.action) {
            ACTION_START_RECORDING -> startRecording()
            ACTION_STOP_RECORDING -> stopRecording()
            else -> startForeground(notificationId, buildNotification())
        }

        return START_STICKY
    }

    /**
     * Начать запись
     */
    private fun startRecording() {
        if (audioRecorder?.isRecording() == false) {
            if (audioRecorder?.startRecording() == true) {
                isRecording = true
                updateNotification("🟢 Идёт запись звонка...")
                Log.d("CallRecordingService", "✅ Запись началась")
            }
        }
    }

    /**
     * Остановить запись
     */
    private fun stopRecording() {
        if (audioRecorder?.isRecording() == true) {
            val filePath = audioRecorder?.stopRecording()
            isRecording = false
            Log.d("CallRecordingService", "✅ Запись остановлена: $filePath")

            // Удаляем уведомление
            CallRecordingNotification.cancelNotification(this)
            stopForeground(STOP_FOREGROUND_REMOVE)

            if (filePath != null) {
                android.widget.Toast.makeText(
                    this,
                    "Запись сохранена",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Построить уведомление
     */
    private fun buildNotification(): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SmartGuard")
            .setContentText("Анализ защиты в процессе")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        return builder.build()
    }

    /**
     * Обновить текст уведомления
     */
    private fun updateNotification(text: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SmartGuard")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    /**
     * Создать канал уведомления
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SmartGuard: Сервис записи звонков",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Канал для фонового сервиса записи звонков"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecorder?.stopRecording()
        Log.d("CallRecordingService", "❌ Сервис остановлен")
    }
}