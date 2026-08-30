package com.example.smartguard.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.smartguard.MainActivity
import com.example.smartguard.R
import com.example.smartguard.database.BlockedNumbersDatabase
import com.example.smartguard.security.SecureStorage
import com.example.smartguard.security.SecurityAudit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private var isProtectionEnabled = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Загружаем состояние защиты
        isProtectionEnabled = SecureStorage.getBoolean(requireContext(), "protection_enabled", true)

        // Находим View
        val statusText = view.findViewById<TextView>(R.id.statusText)
        val enableBtn = view.findViewById<Button>(R.id.enableProtectionBtn)
        val ivShield = view.findViewById<ImageView>(R.id.ivShield)
        val viewPulse = view.findViewById<View>(R.id.viewPulse)
        val cardToggle = view.findViewById<View>(R.id.cardToggleProtection)
        val cardCheck = view.findViewById<View>(R.id.cardCheckNumber)
        val cardMenu = view.findViewById<View>(R.id.cardMenu)

        // Обновляем UI
        updateProtectionUI(statusText, enableBtn, ivShield, viewPulse)

        // Клик по карточке включения/выключения защиты
        cardToggle.setOnClickListener {
            isProtectionEnabled = !isProtectionEnabled
            SecureStorage.putBoolean(requireContext(), "protection_enabled", isProtectionEnabled)
            updateProtectionUI(statusText, enableBtn, ivShield, viewPulse)

            if (isProtectionEnabled) {
                SecurityAudit.logEvent(
                    requireContext(),
                    SecurityAudit.EventType.PROTECTION_ENABLED,
                    "Пользователь включил защиту",
                    SecurityAudit.RiskLevel.LOW
                )
                Toast.makeText(requireContext(), "✅ Защита включена!", Toast.LENGTH_SHORT).show()
            } else {
                SecurityAudit.logEvent(
                    requireContext(),
                    SecurityAudit.EventType.PROTECTION_DISABLED,
                    "Пользователь выключил защиту",
                    SecurityAudit.RiskLevel.MEDIUM
                )
                Toast.makeText(requireContext(), "⚠️ Защита выключена", Toast.LENGTH_SHORT).show()
            }
        }

        // Клик по проверке номера
        cardCheck.setOnClickListener {
            showCheckNumberDialog()
        }

        // Клик по открытию бокового меню
        cardMenu.setOnClickListener {
            (activity as? MainActivity)?.openDrawer()
        }
    }

    private fun updateProtectionUI(
        statusText: TextView,
        enableBtn: Button,
        ivShield: ImageView,
        viewPulse: View
    ) {
        if (isProtectionEnabled) {
            statusText.text = getString(R.string.protection_on)
            statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.shield_active))
            enableBtn.text = getString(R.string.btn_disable_protection)

            // Загружаем GIF с весёлым барсом
            Glide.with(this)
                .asGif()
                .load(R.drawable.happy_bars)
                .into(ivShield)

            // Показываем пульсацию
            viewPulse.visibility = View.VISIBLE
            viewPulse.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.pulse))
        } else {
            statusText.text = getString(R.string.protection_off)
            statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.shield_inactive))
            enableBtn.text = getString(R.string.btn_enable_protection)

            // Загружаем GIF с грустным барсом
            Glide.with(this)
                .asGif()
                .load(R.drawable.sad_bars)
                .into(ivShield)

            // Скрываем пульсацию
            viewPulse.visibility = View.GONE
            viewPulse.clearAnimation()
        }
    }

    private fun showCheckNumberDialog() {
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_PHONE
            hint = "+7XXXXXXXXXX"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Проверить номер")
            .setMessage("Введите номер телефона для проверки по базе мошенников")
            .setView(input)
            .setPositiveButton("Проверить") { dialog, _ ->
                val number = input.text.toString().trim()
                if (number.isNotBlank()) {
                    checkNumber(number)
                } else {
                    Toast.makeText(requireContext(), "Введите номер", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun checkNumber(phoneNumber: String) {
        lifecycleScope.launch {
            val db = BlockedNumbersDatabase.getDatabase(requireContext())
            val blockedNumber = withContext(Dispatchers.IO) {
                db.blockedNumberDao().findByNumber(phoneNumber)
            }

            val isBlocked = blockedNumber != null
            val totalBlocked = withContext(Dispatchers.IO) {
                db.blockedNumberDao().getAllBlockedNumbersOnce().size
            }

            SecurityAudit.logEvent(
                requireContext(),
                SecurityAudit.EventType.USER_ACTION,
                "Проверка номера: $phoneNumber",
                if (isBlocked) SecurityAudit.RiskLevel.HIGH else SecurityAudit.RiskLevel.LOW,
                phoneNumber
            )

            val title = if (isBlocked) "🚨 Номер в чёрном списке!" else "✅ Номер не найден"
            val message = buildString {
                append("📊 Всего номеров в базе: $totalBlocked\n\n")
                append("📞 Проверяемый номер:\n$phoneNumber\n\n")
                if (isBlocked) {
                    append("⚠️ Этот номер находится в базе мошенников!\n")
                    append("Категория: ${blockedNumber?.category ?: "не указана"}\n")
                    append("Добавлен: ${blockedNumber?.dateAdded ?: "неизвестно"}")
                } else {
                    append("✓ Номер не найден в чёрном списке.")
                }
            }

            AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Glide автоматически управляет жизненным циклом
    }
}