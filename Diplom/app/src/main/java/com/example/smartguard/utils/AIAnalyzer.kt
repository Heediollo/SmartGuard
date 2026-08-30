package com.example.smartguard.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AIAnalysisResult(
    val isPhishing: Boolean,
    val category: String,
    val confidence: Int,
    val reason: String
)

object AIAnalyzer {

    private const val YANDEX_GPT_URL = "https://llm.api.cloud.yandex.net/foundationModels/v1/completion"
    private const val API_KEY = "YOUR_YANDEX_API_KEY"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeWithAI(text: String): AIAnalysisResult {
        return withContext(Dispatchers.IO) {
            try {
                val response = callYandexGPT(text)
                parseAIResponse(response)
            } catch (e: Exception) {
                // Логируем ошибку и используем простой AI
                e.printStackTrace()
                analyzeWithSimpleAI(text)
            }
        }
    }

    private suspend fun callYandexGPT(text: String): String {
        val requestBody = JSONObject()
            .put("modelUri", "gpt://b1g.../yandexgpt/latest")
            .put("completionOptions", JSONObject()
                .put("stream", false)
                .put("temperature", 0.1)
                .put("maxTokens", 200)
            )
            .put("messages", JSONArray()
                .put(JSONObject()
                    .put("role", "system")
                    .put("text", "Ты система безопасности для анализа SMS. Определи фишинг. Ответь JSON: {\"is_phishing\": true/false, \"confidence\": 0-100, \"category\": \"bank_scam/government_scam/tech_support/prize_scam/other\", \"reason\": \"причина\"}")
                )
                .put(JSONObject()
                    .put("role", "user")
                    .put("text", "Проанализируй SMS: $text")
                )
            )

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(YANDEX_GPT_URL)
            .post(requestBody.toString().toRequestBody(mediaType))
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Api-Key $API_KEY")
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("Yandex GPT API error: ${response.code}")
        }

        return response.body?.string() ?: throw Exception("Empty response")
    }

    private fun parseAIResponse(response: String): AIAnalysisResult {
        return try {
            val json = JSONObject(response)
            val result = json.getJSONObject("result")
            val alternatives = result.getJSONArray("alternatives")
            val firstAlternative = alternatives.getJSONObject(0)
            val message = firstAlternative.getJSONObject("message")
            val text = message.getString("text")

            val aiJson = JSONObject(text)

            AIAnalysisResult(
                isPhishing = aiJson.getBoolean("is_phishing"),
                category = aiJson.getString("category"),
                confidence = aiJson.getInt("confidence"),
                reason = aiJson.getString("reason")
            )
        } catch (e: Exception) {
            e.printStackTrace()
            AIAnalysisResult(false, "safe", 0, "Ошибка парсинга AI")
        }
    }

    fun analyzeWithSimpleAI(text: String): AIAnalysisResult {
        val lowerText = text.lowercase()

        val phishingWords = listOf("срочно", "блокировка", "подтвердите", "выиграли", "перейдите")

        var matches = 0
        for (word in phishingWords) {
            if (lowerText.contains(word)) matches++
        }

        val confidence = (matches * 25).coerceAtMost(90)

        return AIAnalysisResult(
            isPhishing = confidence >= 50,
            category = if (confidence >= 50) "other" else "safe",
            confidence = confidence,
            reason = "Простой AI анализ"
        )
    }
}