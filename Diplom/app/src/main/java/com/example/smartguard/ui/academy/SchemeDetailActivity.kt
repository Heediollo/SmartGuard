package com.example.smartguard.ui.academy

import android.graphics.BitmapFactory
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.smartguard.R
import com.example.smartguard.ui.academy.ScamScheme
import java.io.IOException

class SchemeDetailActivity : AppCompatActivity() {

    private lateinit var scheme: ScamScheme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scheme_detail)

        scheme = intent.getSerializableExtra("scheme") as? ScamScheme ?: return finish()

        supportActionBar?.title = scheme.title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val webView = findViewById<WebView>(R.id.webView)
        val ivScheme = findViewById<ImageView>(R.id.ivScheme)

        // Загружаем HTML
        val htmlContent = """
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: 'Roboto', sans-serif; margin: 16px; line-height: 1.6; color: #1e1e1e; }
                    h2 { color: #D32F2F; }
                    ul { padding-left: 20px; }
                    li { margin-bottom: 8px; }
                </style>
            </head>
            <body>
                ${scheme.fullDesc}
            </body>
            </html>
        """.trimIndent()

        webView.webViewClient = WebViewClient()
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)

        // Загружаем картинку, если есть
        if (scheme.imageName.isNotEmpty()) {
            try {
                val inputStream = assets.open("images/${scheme.imageName}")
                val bitmap = BitmapFactory.decodeStream(inputStream)
                ivScheme.setImageBitmap(bitmap)
            } catch (e: IOException) {
                ivScheme.setImageResource(R.drawable.ic_menu) // заглушка
            }
        } else {
            ivScheme.setImageResource(R.drawable.ic_menu)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}