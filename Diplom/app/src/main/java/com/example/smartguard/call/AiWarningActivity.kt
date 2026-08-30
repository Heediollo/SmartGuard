package com.example.smartguard.call

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

class AiWarningActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val message = intent.getStringExtra("warning_text") ?: "Обнаружена подозрительная активность"
        val confidence = intent.getIntExtra("confidence", 0)

        // Создаём интерфейс программно (без XML)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(0xFFF44336.toInt()) // красный фон
        }

        val warningTextView = TextView(this).apply {
            text = message
            textSize = 24f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }

        val confidenceTextView = TextView(this).apply {
            text = "Уверенность: $confidence%"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }

        val closeButton = Button(this).apply {
            text = "ЗАКРЫТЬ"
            setBackgroundColor(0xFFFFFFFF.toInt())
            setTextColor(0xFF000000.toInt())
            setOnClickListener { finish() }
        }

        layout.addView(warningTextView)
        layout.addView(confidenceTextView)
        layout.addView(closeButton)

        setContentView(layout)

        // Настройка отступов под вырезы экрана (для Android 11+)
        ViewCompat.setOnApplyWindowInsetsListener(layout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<LinearLayout.LayoutParams> {
                setMargins(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            }
            insets
        }
    }
}