package com.example.smartguard.ui

import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.smartguard.R
import com.example.smartguard.security.SecurityAudit

class HistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // 🔐 ЗАЩИТА ОТ СКРИНШОТОВ И ЗАПИСИ ЭКРАНА
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        title = "📋 Журнал событий"

        val logText = findViewById<TextView>(R.id.logText)

        // Получаем все события
        val events = SecurityAudit.getAllEvents()

        // Формируем текст
        val sb = StringBuilder()
        sb.append("=== ЖУРНАЛ СОБЫТИЙ БЕЗОПАСНОСТИ ===\n\n")
        sb.append("📊 Всего событий: ${events.size}\n\n")

        if (events.isEmpty()) {
            sb.append("📭 Пока нет событий\n\n")
            sb.append("Все действия будут отображаться здесь:\n")
            sb.append("• Включение/выключение защиты\n")
            sb.append("• Обнаружение угроз\n")
            sb.append("• Запрос разрешений\n")
        } else {
            events.forEach { event ->
                // Иконка по уровню риска
                val icon = when (event.riskLevel) {
                    "LOW" -> "🟢"
                    "MEDIUM" -> "🟡"
                    "HIGH" -> "🔴"
                    "CRITICAL" -> "⚫"
                    else -> "⚪"
                }

                sb.append("$icon [${event.timestamp}]\n")
                sb.append("📌 ${event.eventType}\n")
                sb.append("📝 ${event.description}\n")
                sb.append("🔒 Уровень: ${event.riskLevel}\n")
                if (event.phoneNumber.isNotEmpty()) {
                    sb.append("📞 ${event.phoneNumber}\n")
                }
                sb.append("\n-------------------\n\n")
            }
        }

        logText.text = sb.toString()
    }
}