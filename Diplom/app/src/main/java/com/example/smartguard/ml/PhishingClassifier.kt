package com.example.smartguard.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.channels.FileChannel
import kotlin.math.max

class PhishingClassifier(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var tokenizer: Tokenizer? = null  // 🔥 Nullable!
    val maxSequenceLength = 50
    var maxVocabularySize = 5000

    companion object {
        private const val TAG = "PhishingClassifier"

        // 🔥 Пороги для гибридной системы
        private const val HEURISTIC_THRESHOLD = 0.85f      // Если эвристика уверена на 85%+ — сразу блокируем
        private const val ML_CONFIDENCE_THRESHOLD = 0.75f  // Порог для ML-модели
        private const val FINAL_DECISION_THRESHOLD = 0.70f // Итоговый порог решения
    }

    /**
     * Загрузка модели из папки assets/
     */
    fun loadModel(): Boolean {
        return try {
            Log.d(TAG, "⏳ Начинаю загрузку модели...")

            val fd = context.assets.openFd("phishing_model.tflite")
            Log.d(TAG, "📦 Размер файла: ${fd.length} байт")

            val inputStream = FileInputStream(fd.fileDescriptor)
            val channel = inputStream.channel
            val buffer = channel.map(
                FileChannel.MapMode.READ_ONLY,
                fd.startOffset.toLong(),
                fd.length.toLong()
            )

            interpreter = Interpreter(buffer)

            // 🔥 Инициализируем токенизатор
            tokenizer = Tokenizer(context, maxVocabularySize)
            tokenizer?.loadDictionary()

            Log.d(TAG, "✅ Модель успешно загружена (${fd.length / 1024} КБ)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка загрузки модели", e)
            false
        }
    }

    /**
     * 🔥 ГИБРИДНЫЙ АНАЛИЗ: Эвристика + ML-модель
     *
     * Алгоритм:
     * 1. Быстрая проверка по эвристикам (ключевые слова, паттерны)
     * 2. Если эвристика не уверена — запускаем ML-модель
     * 3. Объединяем результаты с калибровкой уверенности
     * 4. Возвращаем финальное решение
     */
    fun analyzeMessage(message: String): Result {
        Log.d(TAG, "🔍 Analyzing: \"$message\"")

        // 🔹 ШАГ 1: Быстрая эвристическая проверка
        val heuristicResult = runHeuristicCheck(message)
        if (heuristicResult.isHighConfidence) {
            Log.d(TAG, "⚡ Heuristic hit: ${heuristicResult.message} (${heuristicResult.confidence * 100}%)")
            return Result(
                isPhishing = heuristicResult.isPhishing,
                confidence = heuristicResult.confidence,
                message = heuristicResult.message,
                source = "heuristic"
            )
        }

        // 🔹 ШАГ 2: Если эвристика не уверена — запускаем ML-модель
        val mlResult = runModelCheck(message)

        // 🔹 ШАГ 3: Объединяем результаты с калибровкой
        val finalResult = combineResults(heuristicResult, mlResult, message)

        Log.d(TAG, "🎯 Final: ${if (finalResult.isPhishing) "ФИШИНГ" else "ОК"} | ${finalResult.confidence * 100}% | src:${finalResult.source}")

        return finalResult
    }

    /**
     * 🔹 ШАГ 1: Быстрая эвристическая проверка
     * Возвращает результат с флагом isHighConfidence
     */
    private fun runHeuristicCheck(message: String): HeuristicResult {
        val lower = message.lowercase()

        // 🔸 Высокоприоритетные фишинговые паттерны
        val highRiskPatterns = listOf(
            "код из смс", "пин.?код", "подтвердите.*карт", "срочно.*переведите",
            "счёт.*заморожен", "компенсация.*подтвердите", "служба.*безопасности.*код",
            "блокировка.*карты", "назовите.*код", "данные.*карты.*подтвердите"
        )

        // 🔸 Высокоприоритетные безопасные паттерны
        val safePatterns = listOf(
            "привет", "как дела", "встретимся", "завтра", "спасибо", "пока",
            "договорились", "ок", "хорошо", "ладно", "понял", "поняла"
        )

        // 🔸 Подсчёт совпадений
        val phishingMatches = highRiskPatterns.count { lower.contains(Regex(it)) }
        val safeMatches = safePatterns.count { lower.contains(it) }

        // 🔸 Расчёт уверенности эвристики
        val totalPatterns = highRiskPatterns.size + safePatterns.size
        val phishingScore = if (totalPatterns > 0) phishingMatches.toFloat() / highRiskPatterns.size else 0f
        val safeScore = if (totalPatterns > 0) safeMatches.toFloat() / safePatterns.size else 0f

        // 🔸 Принятие решения
        return when {
            phishingMatches >= 2 || (phishingMatches == 1 && phishingScore > 0.8f) -> {
                HeuristicResult(
                    isPhishing = true,
                    confidence = max(0.85f, phishingScore + 0.1f),
                    message = "Обнаружена фишинговая угроза!",
                    isHighConfidence = true
                )
            }
            safeMatches >= 2 || (safeMatches == 1 && safeScore > 0.8f) -> {
                HeuristicResult(
                    isPhishing = false,
                    confidence = max(0.85f, safeScore + 0.1f),
                    message = "Сообщение безопасное",
                    isHighConfidence = true
                )
            }
            else -> {
                // 🔸 Эвристика не уверена — передаём дальше
                HeuristicResult(
                    isPhishing = phishingScore > safeScore,
                    confidence = max(phishingScore, safeScore),
                    message = if (phishingScore > safeScore) "Подозрительное сообщение" else "Нейтральное сообщение",
                    isHighConfidence = false
                )
            }
        }
    }

    /**
     * 🔹 ШАГ 2: Проверка через ML-модель
     */
    private fun runModelCheck(message: String): MLResult {
        val tokenizerInstance = tokenizer
        if (interpreter == null || tokenizerInstance == null || !tokenizerInstance.isLoaded) {
            Log.w(TAG, "⚠️ ML model not ready")
            return MLResult(isPhishing = false, confidence = 0f, message = "ML not ready")
        }

        return try {
            val inputArray = prepareInputForModel(message, tokenizerInstance)
            val outputArray = Array(1) { FloatArray(2) }
            interpreter?.run(inputArray, outputArray)

            val probabilities = outputArray[0]
            // 🔥 [0] = phishing, [1] = safe (проверь свою модель!)
            val phishingProb = probabilities[0]
            val safeProb = probabilities[1]

            val isPhishing = phishingProb > safeProb
            val confidence = max(phishingProb, safeProb)  // 🔥 Доля 0.0-1.0

            MLResult(
                isPhishing = isPhishing,
                confidence = confidence,
                message = if (isPhishing) "Обнаружена фишинговая угроза!" else "Сообщение безопасное"
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ ML inference error", e)
            MLResult(isPhishing = false, confidence = 0f, message = "ML error")
        }
    }

    /**
     * 🔹 ШАГ 3: Объединение результатов с калибровкой
     */
    private fun combineResults(heuristic: HeuristicResult, ml: MLResult, message: String): Result {
        return when {
            // 🔸 Эвристика была уверена — доверяем ей
            heuristic.isHighConfidence -> {
                Result(
                    isPhishing = heuristic.isPhishing,
                    confidence = heuristic.confidence,
                    message = heuristic.message,
                    source = "heuristic"
                )
            }
            // 🔸 Эвристика не уверена, но модель уверена — доверяем модели
            ml.confidence > ML_CONFIDENCE_THRESHOLD -> {
                // 🔸 Калибровка: немного снижаем уверенность модели, если эвристика сомневается
                val calibratedConfidence = ml.confidence * 0.95f
                Result(
                    isPhishing = ml.isPhishing,
                    confidence = calibratedConfidence,
                    message = ml.message,
                    source = "ml"
                )
            }
            // 🔸 Оба сомневаются — консервативное решение (безопаснее заблокировать)
            else -> {
                val avgConfidence = (heuristic.confidence + ml.confidence) / 2f
                val isPhishing = avgConfidence > FINAL_DECISION_THRESHOLD &&
                        (heuristic.isPhishing || ml.isPhishing)

                Result(
                    isPhishing = isPhishing,
                    confidence = avgConfidence,
                    message = if (isPhishing) "Возможная фишинговая угроза" else "Сообщение проверено",
                    source = "combined"
                )
            }
        }
    }

    private fun prepareInputForModel(text: String, tokenizer: Tokenizer): Array<FloatArray> {
        val tokens = tokenizer.tokenizeText(text)
        val inputArray = Array(1) { FloatArray(maxSequenceLength) }
        for (i in tokens.indices) {
            if (i < maxSequenceLength) {
                inputArray[0][i] = tokens[i].toFloat()
            }
        }
        return inputArray
    }

    fun getTokenizer(): Tokenizer? = tokenizer
    fun isReady(): Boolean = interpreter != null && tokenizer?.isLoaded == true
}

// 🔥 Данные для эвристического слоя
data class HeuristicResult(
    val isPhishing: Boolean,
    val confidence: Float,      // 0.0-1.0
    val message: String,
    val isHighConfidence: Boolean  // 🔥 Флаг: можно ли доверять этому результату
)

// 🔥 Данные для ML-слоя
data class MLResult(
    val isPhishing: Boolean,
    val confidence: Float,      // 0.0-1.0
    val message: String
)

// 🔥 Финальный результат (возвращается наружу)
data class Result(
    val isPhishing: Boolean,
    val confidence: Float,      // 🔥 Всегда 0.0-1.0 (НЕ проценты!)
    val message: String,
    val source: String = "unknown"  // "heuristic" | "ml" | "combined"
)

// 🔥 Токенизатор (без изменений, но с улучшенной инициализацией)
class Tokenizer(private val context: Context, maxWords: Int = 5000) {
    private val wordToIndex = HashMap<String, Int>()
    private var vocabularySize = 0
    private val vocabLimit = maxWords
    private var _isLoaded = false
    val isLoaded: Boolean get() = _isLoaded

    companion object {
        private const val TAG = "Tokenizer"
    }

    fun loadDictionary(): Boolean {
        return try {
            initializeDefaultDictionary()
            vocabularySize = vocabLimit.coerceAtMost(wordToIndex.size + 1)
            _isLoaded = true
            Log.d(TAG, "✅ Dictionary loaded: $vocabularySize words")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка загрузки словаря", e)
            false
        }
    }

    fun tokenizeText(text: String): List<Int> {
        val cleanedText = cleanText(text)
        val words = cleanedText.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        val indices = mutableListOf<Int>()
        for (word in words) {
            val lowerWord = word.lowercase()
            val index = wordToIndex[lowerWord] ?: 0
            indices.add(index)
        }
        while (indices.size < 50) indices.add(0)
        return indices.take(50)
    }

    private fun cleanText(text: String): String {
        return text.lowercase()
            .replace("[^a-zа-яё'\\d\\s]".toRegex(), " ")
            .trim()
    }

    private fun initializeDefaultDictionary() {
        val phishingKeywords = listOf(
            "карта", "заблокирована", "подтвердите", "оплатите", "срочно", "немедленно",
            "аккаунт", "взломан", "пароль", "код", "проверьте", "банковская", "перевод",
            "кредит", "баланс", "восстановить", "безопасность", "мошенничество",
            "бұғатталған", "растаңыз", "дереу", "төлеңіз", "шот", "ескертіңіз", "мерзімі",
            "kaspi", "halyk", "freedom", "bcc", "egov", "beeline", "kcell", "kazakhtelecom",
            "spam", "free money", "click here", "urgent", "verify account", "password reset"
        )
        val normalKeywords = listOf(
            "привет", "как дела", "встретимся", "завтра", "магазин", "продукты", "работа",
            "семья", "покупки", "телефон", "компьютер", "интернет", "дом",
            "сәлем", "қалайсың", "ертең", "дүкен", "еңбек", "отбасы", "жақсы күн",
            "hello", "goodbye", "meeting", "work", "office", "email", "phone",
            "hi", "see you", "tomorrow", "please", "thank you", "sorry"
        )
        phishingKeywords.forEachIndexed { index, word -> wordToIndex[word] = index + 1 }
        normalKeywords.forEachIndexed { index, word ->
            if (!wordToIndex.containsKey(word)) {
                wordToIndex[word] = phishingKeywords.size + index + 1
            }
        }
        Log.d(TAG, "✅ Базовый словарь создан (${wordToIndex.size} слов)")
    }
}