package com.example.smartguard.ui.academy

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.smartguard.R
import java.io.IOException

class TestActivity : AppCompatActivity() {

    private lateinit var tvQuestion: TextView
    private lateinit var ivExample: ImageView
    private lateinit var optionsContainer: LinearLayout
    private lateinit var btnNext: Button

    private var currentIndex = 0
    private var correctAnswers = 0
    private lateinit var questions: List<ModuleDetailActivity.TestQuestion>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        val title = intent.getStringExtra("test_title") ?: "Тест"
        supportActionBar?.title = title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        tvQuestion = findViewById(R.id.tvQuestion)
        ivExample = findViewById(R.id.ivExample)
        optionsContainer = findViewById(R.id.optionsContainer)
        btnNext = findViewById(R.id.btnNext)

        @Suppress("UNCHECKED_CAST")
        questions = intent.getSerializableExtra("questions") as? List<ModuleDetailActivity.TestQuestion> ?: emptyList()

        if (questions.isEmpty()) {
            Toast.makeText(this, "Нет вопросов для теста", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        btnNext.setOnClickListener {
            if (currentIndex < questions.size - 1) {
                currentIndex++
                showQuestion(currentIndex)
                btnNext.isEnabled = false
            } else {
                showResult()
            }
        }

        showQuestion(0)
    }

    private fun showQuestion(index: Int) {
        val q = questions[index]
        tvQuestion.text = "${index + 1} из ${questions.size}. ${q.question}"

        // Загружаем картинку, если она указана
        if (q.imageName.isNotEmpty()) {
            try {
                val inputStream = assets.open("images/${q.imageName}")
                val bitmap = BitmapFactory.decodeStream(inputStream)
                ivExample.setImageBitmap(bitmap)
                ivExample.visibility = View.VISIBLE
            } catch (e: IOException) {
                // 🔥 Показываем Toast с точным именем отсутствующего файла
                Toast.makeText(this, "❌ Файл не найден: '${q.imageName}'", Toast.LENGTH_LONG).show()
                ivExample.visibility = View.GONE
            }
        } else {
            ivExample.visibility = View.GONE
        }

        optionsContainer.removeAllViews()

        q.options.forEachIndexed { i, opt ->
            val view = LayoutInflater.from(this).inflate(R.layout.item_test_option, optionsContainer, false)
            val rb = view.findViewById<RadioButton>(R.id.rbOption)
            val tvExplanation = view.findViewById<TextView>(R.id.tvExplanation)

            rb.text = opt
            rb.setOnClickListener {
                if (btnNext.isEnabled) return@setOnClickListener

                val isCorrect = (i == q.correctAnswerIndex)
                if (isCorrect) correctAnswers++

                if (isCorrect) {
                    rb.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
                } else {
                    rb.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
                    val correctView = optionsContainer.getChildAt(q.correctAnswerIndex)
                    correctView.findViewById<RadioButton>(R.id.rbOption)
                        .setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
                }

                tvExplanation.text = q.explanations[i]
                tvExplanation.visibility = View.VISIBLE

                for (j in 0 until optionsContainer.childCount) {
                    optionsContainer.getChildAt(j).findViewById<RadioButton>(R.id.rbOption).isEnabled = false
                }
                btnNext.isEnabled = true
            }

            optionsContainer.addView(view)
        }
    }

    private fun showResult() {
        val message = when (correctAnswers) {
            questions.size -> "🎉 Идеально! Вы настоящий эксперт!"
            in (questions.size / 2) until questions.size -> "👍 Хорошо! Вы усвоили основные приёмы."
            else -> "📚 Рекомендуем повторить уроки."
        }
        AlertDialog.Builder(this)
            .setTitle("Тест завершён")
            .setMessage("$message\n\nПравильных ответов: $correctAnswers из ${questions.size}")
            .setPositiveButton("ОК") { _, _ -> finish() }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}