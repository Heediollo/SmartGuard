package com.example.smartguard.ml

import android.content.Context
import android.util.Log

/**
 * 🔥 Singleton менеджер для AI-классификатора
 * Гарантирует, что модель загружается ОДИН РАЗ для всего приложения
 */
object ClassifierManager {

    private const val TAG = "ClassifierManager"

    private var classifier: PhishingClassifier? = null
    private var isInitialized = false

    /**
     * 🔥 Инициализация классификатора (вызывается ОДИН РАЗ при старте приложения)
     */
    fun initialize(context: Context): Boolean {
        if (isInitialized && classifier != null) {
            Log.d(TAG, "✅ Classifier already initialized")
            return true
        }

        return try {
            Log.d(TAG, "🔄 Initializing classifier...")
            classifier = PhishingClassifier(context)
            val loaded = classifier?.loadModel()

            if (loaded == true) {
                isInitialized = true
                Log.d(TAG, "✅ Classifier initialized successfully")
                true
            } else {
                Log.e(TAG, "❌ Failed to load model")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing classifier", e)
            false
        }
    }

    /**
     * 🔥 Получение экземпляра классификатора
     * @return PhishingClassifier или null, если не инициализирован
     */
    fun getClassifier(): PhishingClassifier? {
        if (!isInitialized) {
            Log.w(TAG, "⚠️ Classifier not initialized yet")
        }
        return classifier
    }

    /**
     * 🔥 Проверка готовности классификатора
     */
    fun isReady(): Boolean {
        return isInitialized && classifier?.isReady() == true
    }

    /**
     * 🔥 Очистка ресурсов (при закрытии приложения)
     */
    fun shutdown() {
        classifier = null
        isInitialized = false
        Log.d(TAG, "🛑 Classifier shutdown complete")
    }
}