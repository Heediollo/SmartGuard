package com.example.smartguard.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.telephony.TelephonyManager

/**
 * Слушает события входящих и исходящих звонков
 */
class CallBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CallBroadcastReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        Log.d(TAG, "📡 Получено событие: $action")

        when (action) {
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                handlePhoneStateChanged(context, intent)
            }
        }
    }

    /**
     * Обработка состояния телефона (звонок идет/завершился)
     */
    private fun handlePhoneStateChanged(context: Context, intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return

        Log.d(TAG, "📞 Состояние телефона: $state")

        // Начало звонка → запускаем запись
        if (state == TelephonyManager.EXTRA_STATE_OFFHOOK ||
            state == TelephonyManager.EXTRA_STATE_RINGING) {

            startCallRecording(context)
        }
        // Конец звонка → останавливаем запись
        else if (state == TelephonyManager.EXTRA_STATE_IDLE) {
            stopCallRecording(context)
        }
    }

    /**
     * Начать запись звонка
     */
    private fun startCallRecording(context: Context) {
        Log.d(TAG, "🟢 Начинаю запись звонка...")

        // Запускаем сервис записи
        val serviceIntent = Intent(context, CallRecordingService::class.java).apply {
            action = CallRecordingService.ACTION_START_RECORDING
        }

        // Исправлено: Убрли лишнюю проверку версии SDK
        context.startForegroundService(serviceIntent)

        android.widget.Toast.makeText(
            context,
            "Запись звонка началась...",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Остановить запись звонка
     */
    private fun stopCallRecording(context: Context) {
        Log.d(TAG, "🔴 Останавливаю запись звонка...")

        // Останавливаем сервис записи
        val serviceIntent = Intent(context, CallRecordingService::class.java).apply {
            action = CallRecordingService.ACTION_STOP_RECORDING
        }

        context.startService(serviceIntent)

        android.widget.Toast.makeText(
            context,
            "Запись завершена",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}