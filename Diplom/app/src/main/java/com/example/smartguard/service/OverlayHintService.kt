package com.example.smartguard.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView

class OverlayHintService(private val context: Context) {

    companion object {
        private const val TAG = "OverlayHintService"
    }

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    private var isOverlayShowing = false
    private val handler = Handler(Looper.getMainLooper())

    // Сделали метод публичным для проверки из сервиса
    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun showHint(message: String, priority: HintPriority) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post { showHintInternal(message, priority, null, null) }
            return
        }
        showHintInternal(message, priority, null, null)
    }

    fun showInteractiveQuestion(
        title: String,
        options: List<String>,
        onAnswer: (String) -> Unit
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post { showInteractiveInternal(title, options, onAnswer) }
            return
        }
        showInteractiveInternal(title, options, onAnswer)
    }

    private fun showHintInternal(
        message: String,
        priority: HintPriority,
        options: List<String>?,
        onAnswer: ((String) -> Unit)?
    ) {
        if (!hasOverlayPermission()) {
            Log.e(TAG, "❌ SYSTEM_ALERT_WINDOW permission is NOT granted!")
            return
        }

        try {
            removeHintInternal()

            if (windowManager == null) {
                windowManager = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = 200
                width = (context.resources.displayMetrics.widthPixels * 0.9).toInt()
            }

            overlayView = createOverlayView(message, priority, options, onAnswer)

            val wm = windowManager
            val view = overlayView
            if (wm != null && view != null) {
                wm.addView(view, params)
                isOverlayShowing = true
                Log.d(TAG, "✅ Overlay shown")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to show overlay: ${e.message}", e)
            isOverlayShowing = false
        }
    }

    private fun showInteractiveInternal(
        title: String,
        options: List<String>,
        onAnswer: (String) -> Unit
    ) {
        showHintInternal(title, HintPriority.HIGH, options, onAnswer)
    }

    private fun createOverlayView(
        message: String,
        priority: HintPriority,
        options: List<String>?,
        onAnswer: ((String) -> Unit)?
    ): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.parseColor("#E6000000"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val textView = TextView(context).apply {
            text = when (priority) {
                HintPriority.HIGH -> "🚨 $message"
                HintPriority.MEDIUM -> "⚠️ $message"
                HintPriority.LOW -> "💡 $message"
                else -> message
            }
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 20)
        }
        container.addView(textView)

        if (options != null && onAnswer != null) {
            val gridLayout = GridLayout(context).apply {
                columnCount = 2
                rowCount = (options.size + 1) / 2
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            options.forEach { option ->
                val button = Button(context).apply {
                    text = option
                    setBackgroundColor(Color.parseColor("#CC1976D2"))
                    setTextColor(Color.WHITE)
                    setOnClickListener {
                        removeHint()
                        onAnswer(option)
                    }
                }
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(8, 8, 8, 8)
                }
                button.layoutParams = params
                gridLayout.addView(button)
            }

            container.addView(gridLayout)
        }

        return container
    }

    fun removeHint() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post { removeHintInternal() }
            return
        }
        removeHintInternal()
    }

    private fun removeHintInternal() {
        try {
            val wm = windowManager
            val view = overlayView
            if (wm != null && view != null && isOverlayShowing) {
                wm.removeView(view)
                overlayView = null
                isOverlayShowing = false
                Log.d(TAG, "✅ Overlay removed")
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Overlay removal error: ${e.message}")
            overlayView = null
            isOverlayShowing = false
        }
    }

    enum class HintPriority {
        LOW, MEDIUM, HIGH
    }
}