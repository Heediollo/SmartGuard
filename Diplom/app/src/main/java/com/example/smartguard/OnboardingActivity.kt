package com.example.smartguard

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.smartguard.databinding.ActivityOnboardingBinding
import com.example.smartguard.security.SecureStorage

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var adapter: OnboardingAdapter

    private val onboardingItems = listOf(
        OnboardingItem(
            "Добро пожаловать!",
            "SmartGuard — ваш персональный защитник от телефонного и SMS-мошенничества.\n\nПриложение использует ИИ для анализа звонков и сообщений в реальном времени.",
            "🛡️",
            "#1976D2"
        ),
        OnboardingItem(
            "Умная защита",
            "Выберите уровень защиты:\n\n• Базовый — проверка по базе спама\n• Стандарт + анализ SMS\n• Максимум + ИИ-анализ звонков",
            "⚙️",
            "#2E7D32"
        ),
        OnboardingItem(
            "Разрешения",
            "Для работы нужны разрешения:\n\n📞 Телефон — проверка звонков\n📨 SMS — анализ сообщений\n🎤 Микрофон — ИИ-анализ разговора",
            "🔐",
            "#F57C00"
        ),
        OnboardingItem(
            "Готово!",
            "Защита настроена!\n\nПриложение будет работать в фоне и предупреждать об угрозах в реальном времени.\n\nНажмите «Начать защиту»",
            "✅",
            "#7B1FA2"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Проверяем, был ли уже показан онбординг
        if (SecureStorage.getBoolean(this, "onboarding_completed", false)) {
            goToMainActivity()
            return
        }

        setupViewPager()
        setupButtons()
    }

    private fun setupViewPager() {
        adapter = OnboardingAdapter(onboardingItems)
        binding.viewPager.adapter = adapter
    }

    private fun setupButtons() {
        binding.nextButton.setOnClickListener {
            val currentItem = binding.viewPager.currentItem

            if (currentItem < onboardingItems.size - 1) {
                binding.viewPager.currentItem = currentItem + 1
            } else {
                SecureStorage.putBoolean(this, "onboarding_completed", true)
                goToMainActivity()
            }
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == onboardingItems.size - 1) {
                    binding.nextButton.text = "Начать защиту"
                    binding.nextButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        getColor(android.R.color.holo_green_dark)
                    )
                } else {
                    binding.nextButton.text = "Далее"
                    binding.nextButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        getColor(android.R.color.holo_blue_dark)
                    )
                }
            }
        })
    }

    private fun goToMainActivity() {
        try {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}