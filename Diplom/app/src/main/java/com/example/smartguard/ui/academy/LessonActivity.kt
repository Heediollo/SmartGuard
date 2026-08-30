package com.example.smartguard.ui.academy

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.example.smartguard.R

class LessonActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lesson)

        val lessonId = intent.getStringExtra("lesson_id") ?: "pause_1"
        val title = intent.getStringExtra("lesson_title") ?: "Урок"

        supportActionBar?.title = title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        webView = findViewById(R.id.webView)
        webView.webViewClient = WebViewClient()
        webView.settings.apply {
            javaScriptEnabled = false // если не нужно
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
        }

        // Загружаем локальный HTML-файл из assets/lessons/
        webView.loadUrl("file:///android_asset/lessons/${lessonId}.html")
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}