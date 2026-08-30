package com.example.smartguard.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.smartguard.R

class LinkCheckerFragment : Fragment() {

    private val officialDomains = listOf(
        "kaspi.kz",
        "halykbank.kz",
        "forte.bank",
        "jusan.kz",
        "egov.kz"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_link_checker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etUrl = view.findViewById<EditText>(R.id.etUrl)
        val btnCheck = view.findViewById<Button>(R.id.btnCheck)
        val tvResult = view.findViewById<TextView>(R.id.tvResult)

        btnCheck.setOnClickListener {
            val url = etUrl.text.toString().trim()
            if (url.isEmpty()) {
                tvResult.text = "Введите ссылку"
                return@setOnClickListener
            }

            val result = checkUrl(url)
            tvResult.text = result.message
            tvResult.setTextColor(ContextCompat.getColor(requireContext(), result.color))
        }
    }

    private fun checkUrl(url: String): CheckResult {
        val host = try {
            java.net.URI(url).host ?: return CheckResult("Некорректная ссылка", R.color.shield_inactive)
        } catch (e: Exception) {
            return CheckResult("Некорректная ссылка", R.color.shield_inactive)
        }

        // Проверяем на точное совпадение с официальным доменом
        if (officialDomains.any { host.equals(it, ignoreCase = true) || host.endsWith(".$it") }) {
            return CheckResult("✅ Официальный сайт", R.color.shield_active)
        }

        // Проверяем на подозрительные похожие домены (фишинг)
        for (official in officialDomains) {
            if (host.contains(official.replace(".", "")) && !host.endsWith(".$official")) {
                return CheckResult("⚠️ Подозрительная ссылка! Возможно, фишинг.", R.color.secondary)
            }
            // Проверка на замену символов (кириллица)
            if (host.matches(Regex(".*[а-яА-Я].*")) && host.lowercase().contains(official)) {
                return CheckResult("⚠️ Подозрительная ссылка! Используются русские буквы.", R.color.secondary)
            }
        }

        return CheckResult("❓ Неизвестный сайт. Будьте осторожны.", R.color.text_secondary)
    }

    data class CheckResult(val message: String, val color: Int)
}