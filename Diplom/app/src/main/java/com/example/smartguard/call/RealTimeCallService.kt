package com.example.smartguard.call

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.smartguard.MainActivity
import com.example.smartguard.ai.GuidanceManager
import com.example.smartguard.ai.HybridCallAnalyzer
import com.example.smartguard.ai.HybridCallAnalyzer.ThreatLevel
import com.example.smartguard.ai.Scenario
import com.example.smartguard.database.BlockedCallHistory
import com.example.smartguard.database.BlockedNumbersDatabase
import com.example.smartguard.ml.ClassifierManager
import com.example.smartguard.ml.PhishingClassifier
import com.example.smartguard.security.SecureStorage
import com.example.smartguard.security.SecurityAudit
import com.example.smartguard.service.OverlayHintService
import com.example.smartguard.service.OverlayHintService.HintPriority
import com.example.smartguard.utils.ContactsHelper
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("MissingPermission", "DEPRECATION", "UnusedVariable", "MemberVisibilityCanBePrivate")
class RealTimeCallService : Service() {

    companion object {
        const val ACTION_INIT = "ACTION_INIT"
        const val ACTION_START_ANALYSIS = "ACTION_START_ANALYSIS"
        const val ACTION_STOP_ANALYSIS = "ACTION_STOP_ANALYSIS"
        private const val NOTIFICATION_ID = 5001
        private const val NOTIFICATION_WARNING_ID = 5002
        private const val CHANNEL_ID = "smartguard_ai_analysis"
        private const val TAG = "RealTimeCallService"
        const val TEST_MODE = false
        var instance: RealTimeCallService? = null
        var isCallActive = AtomicBoolean(false)
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var audioRecord: AudioRecord? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var overlayService: OverlayHintService? = null
    private val classifier: PhishingClassifier? get() = ClassifierManager.getClassifier()
    private lateinit var telephonyManager: TelephonyManager
    private val textBuffer = StringBuilder()
    private var isInCall = false
    private var pendingAssistantForUnknownCall = false

    private val answerPath = mutableListOf<String>()

    private val callStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    Log.d(TAG, "📞 Phone ringing: $phoneNumber")
                    pendingAssistantForUnknownCall = false
                    phoneNumber?.let { number ->
                        serviceScope.launch {
                            if (isNumberBlocked(number)) {
                                showBlockedCallNotification(number)
                                return@launch
                            }
                            if (!ContactsHelper.isKnownNumber(number, this@RealTimeCallService)) {
                                Log.d(TAG, "Неизвестный номер, готовим ассистента")
                                withContext(Dispatchers.Main) {
                                    overlayService?.showHint(
                                        "📞 Звонок с неизвестного номера. Будьте внимательны.\n" +
                                                "Советую включить громкую связь и отвечать на мои вопросы по ходу разговора.",
                                        HintPriority.MEDIUM
                                    )
                                }
                                pendingAssistantForUnknownCall = true
                            } else {
                                Log.d(TAG, "Номер известен, ассистент не требуется")
                            }
                        }
                    }
                }
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    if (!isInCall) {
                        isInCall = true
                        startCallAnalysis()
                        if (pendingAssistantForUnknownCall) {
                            Log.d(TAG, "Запуск интерактивного ассистента для неизвестного номера")
                            pendingAssistantForUnknownCall = false
                            serviceScope.launch { withContext(Dispatchers.Main) { startInteractiveAssistant() } }
                        }
                    }
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    // Всегда скрываем оверлей, даже если звонок не был принят
                    serviceScope.launch {
                        withContext(Dispatchers.Main) {
                            overlayService?.removeHint()
                        }
                    }
                    if (isInCall) {
                        isInCall = false
                        stopCallAnalysis()
                    }
                    pendingAssistantForUnknownCall = false
                    answerPath.clear()
                    Log.d(TAG, "Звонок завершён, очистка состояния")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "Сервис создан")
        GuidanceManager.initialize(this)
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        createNotificationChannel()
        initSpeechRecognizer()
        overlayService = OverlayHintService(this)
    }

    private suspend fun startInteractiveAssistant() {
        val prefs = SecureStorage.getEncryptedPrefs(this)
        val isEnabled = prefs.getBoolean("interactive_assistant_enabled", true)
        Log.d(TAG, "Интерактивный ассистент включен в настройках? $isEnabled")
        if (!isEnabled) {
            Log.d(TAG, "Интерактивный ассистент отключен в настройках")
            return
        }

        if (!overlayService?.hasOverlayPermission()!!) {
            Log.e(TAG, "Нет разрешения SYSTEM_ALERT_WINDOW, ассистент не может показывать подсказки")
            return
        }

        withContext(Dispatchers.Main) {
            answerPath.clear()
            Log.d(TAG, "Показываем первый вопрос ассистента")
            overlayService?.showInteractiveQuestion(
                "Кем представился собеседник?",
                listOf("Банк", "Полиция", "eGov/ЦОН", "Родственник", "Начальник", "Оператор связи", "Больница", "Другое")
            ) { answer ->
                Log.d(TAG, "Получен ответ: $answer")
                serviceScope.launch { handleCategoryChoice(answer) }
            }
        }
    }

    private suspend fun handleCategoryChoice(answer: String) {
        Log.d(TAG, "Обработка категории: $answer")
        val scenario = GuidanceManager.getScenario(answer)
        if (scenario == null) {
            Log.w(TAG, "Сценарий для '$answer' не найден, показываем дефолтный совет")
            withContext(Dispatchers.Main) {
                overlayService?.showHint(GuidanceManager.getDefaultAdvice(), HintPriority.MEDIUM)
            }
            return
        }
        // Исправлено: убрали .name
        Log.d(TAG, "Найден сценарий для ответа: $answer")
        withContext(Dispatchers.Main) {
            overlayService?.showHint(scenario.initialMessage, HintPriority.HIGH)
        }
        askNextQuestion(scenario, emptyList())
    }

    private suspend fun askNextQuestion(scenario: Scenario, previousAnswers: List<String>) {
        Log.d(TAG, "Запрос следующего вопроса, предыдущие ответы: $previousAnswers")
        val nextStep = GuidanceManager.getNextStep(scenario, previousAnswers)
        if (nextStep == null || nextStep.options.isEmpty()) {
            val finalMessage = getFinalMessage(scenario, previousAnswers)
            Log.d(TAG, "Достигнут конец сценария, финальное сообщение: $finalMessage")
            withContext(Dispatchers.Main) {
                overlayService?.showHint(finalMessage, HintPriority.MEDIUM)
            }
            return
        }
        Log.d(TAG, "Следующий вопрос: ${nextStep.question}, варианты: ${nextStep.options}")
        withContext(Dispatchers.Main) {
            overlayService?.showInteractiveQuestion(nextStep.question, nextStep.options) { answer ->
                serviceScope.launch {
                    val newAnswers = previousAnswers + answer
                    askNextQuestion(scenario, newAnswers)
                }
            }
        }
    }

    private fun getFinalMessage(scenario: Scenario, answers: List<String>): String {
        if (answers.isEmpty()) return GuidanceManager.getDefaultAdvice()
        var step = scenario.steps.first()
        var lastResponse: com.example.smartguard.ai.Response? = null
        for (ans in answers) {
            lastResponse = step.responses[ans] ?: return GuidanceManager.getDefaultAdvice()
            step = lastResponse.nextStep ?: return lastResponse.message
        }
        return lastResponse?.message ?: GuidanceManager.getDefaultAdvice()
    }

    // ✅ Исправленная нормализация (единая для всего приложения)
    private fun normalizePhoneNumber(number: String): String {
        val cleaned = number.replace("[^+0-9]".toRegex(), "")
        return when {
            cleaned.startsWith("+") -> cleaned
            cleaned.startsWith("8") && cleaned.length == 11 -> "+7" + cleaned.substring(1)
            cleaned.length == 11 && (cleaned.startsWith("7") || cleaned.startsWith("8")) -> "+7" + cleaned.substring(1)
            cleaned.length == 10 -> "+7" + cleaned
            else -> "+$cleaned"
        }
    }

    private suspend fun isNumberBlocked(phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) return false
        val normalized = normalizePhoneNumber(phoneNumber)
        Log.d(TAG, "Проверка номера: исходный=$phoneNumber, нормализованный=$normalized")
        return try {
            val db = BlockedNumbersDatabase.getDatabase(this)
            db.blockedNumberDao().isNumberBlocked(normalized)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking blocked number: ${e.message}", e)
            false
        }
    }

    private fun showBlockedCallNotification(phoneNumber: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚫 Заблокирован звонок")
            .setContentText("Мошеннический номер: $phoneNumber")
            .setSmallIcon(android.R.drawable.ic_lock_power_off)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID + 100, notification)
        SecurityAudit.logEvent(this, SecurityAudit.EventType.CALL_BLOCKED, "Заблокирован входящий звонок с номера $phoneNumber", SecurityAudit.RiskLevel.HIGH)

        serviceScope.launch {
            try {
                val db = BlockedNumbersDatabase.getDatabase(this@RealTimeCallService)
                val history = BlockedCallHistory(phoneNumber = phoneNumber)
                db.blockedCallHistoryDao().insert(history)
                Log.d(TAG, "✅ Запись о блокировке добавлена в историю")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Ошибка сохранения истории: ${e.message}")
            }
        }
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { Log.d(TAG, "🎤 Ready for speech") }
                override fun onBeginningOfSpeech() { Log.d(TAG, "🗣️ Speech started") }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { Log.d(TAG, "🔚 Speech ended") }
                override fun onError(error: Int) {
                    Log.w(TAG, "⚠️ SpeechRecognizer error: $error")
                    if (isCallActive.get() && !TEST_MODE) startListening()
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        Log.d(TAG, "📝 Recognized: \"$text\"")
                        processTranscribedText(text)
                    }
                    if (isCallActive.get() && !TEST_MODE) startListening()
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!partial.isNullOrEmpty()) {
                        val text = partial[0]
                        if (text.length > 10) processTranscribedText(text)
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            Log.d(TAG, "✅ SpeechRecognizer initialized with offline priority")
        } else {
            Log.w(TAG, "⚠️ SpeechRecognizer not available on this device")
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) putExtra("android.speech.extra.DIRECT_ACCESS", true)
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start listening: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        when (intent?.action) {
            ACTION_INIT -> { if (TEST_MODE) startCallAnalysis() }
            ACTION_START_ANALYSIS -> startCallAnalysis()
            ACTION_STOP_ANALYSIS -> stopCallAnalysis()
        }
        return START_STICKY
    }

    private fun startCallAnalysis() {
        if (!TEST_MODE && isCallActive.get()) return
        isCallActive.set(true)

        val prefs = SecureStorage.getEncryptedPrefs(this)
        val voiceEnabled = prefs.getBoolean("voice_analysis_enabled", false)
        if (!voiceEnabled) {
            Log.d(TAG, "Голосовой анализ отключен в настройках")
            updateNotification("Ожидание звонка...")
            return
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "❌ No RECORD_AUDIO permission")
            return
        }

        if (!TEST_MODE) {
            setupAudioRecording()
            startListening()
        } else {
            serviceScope.launch { simulateTestAnalysis() }
        }
        updateNotification("Анализ разговора активен...")
    }

    private fun setupAudioRecording() {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize)
        if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) audioRecord?.startRecording()
    }

    private fun processTranscribedText(text: String) {
        if (text.isBlank()) return
        textBuffer.append(" $text")
        val fullText = textBuffer.toString().trim()
        if (fullText.isNotEmpty()) {
            val result = HybridCallAnalyzer.analyze(fullText, ClassifierManager.getClassifier())
            if (result.isThreat && (result.threatLevel == ThreatLevel.CRITICAL || result.threatLevel == ThreatLevel.HIGH)) {
                showSmartHint(result.hint, result.threatLevel)
            }
            if (textBuffer.length > 300) textBuffer.delete(0, textBuffer.length - 100)
        }
    }

    private fun showSmartHint(message: String, level: ThreatLevel) {
        serviceScope.launch {
            withContext(Dispatchers.Main) {
                val priority = when (level) {
                    ThreatLevel.CRITICAL -> HintPriority.HIGH
                    ThreatLevel.HIGH -> HintPriority.MEDIUM
                    else -> HintPriority.LOW
                }
                overlayService?.showHint(message, priority)
                showNotificationWarning(message)
            }
        }
    }

    private suspend fun simulateTestAnalysis() {
        val phrases = listOf("подтвердите код из смс", "переведите деньги", "пин код", "компенсация", "установите приложение")
        while (isCallActive.get() && serviceScope.isActive) {
            if (Math.random() < 0.4) processTranscribedText(phrases.random())
            delay(3000 + (Math.random() * 2000).toLong())
        }
    }

    private fun showNotificationWarning(message: String) {
        val n = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚨 SmartGuard AI")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_WARNING_ID, n)
    }

    private fun stopCallAnalysis() {
        if (!isCallActive.get()) return
        isCallActive.set(false)
        speechRecognizer?.stopListening()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        serviceScope.launch { withContext(Dispatchers.Main) { overlayService?.removeHint() } }
        textBuffer.clear()
        updateNotification("Ожидание звонка...")
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val contentText = if (isCallActive.get()) "🎤 Анализ разговора активен..." else "🛡️ SmartGuard готов к защите"
        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ SmartGuard AI")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "SmartGuard AI Analysis", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Real-time phishing detection during calls"
                setShowBadge(false); enableVibration(false); enableLights(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopCallAnalysis()
        speechRecognizer?.destroy()
        overlayService?.removeHint()
        telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_NONE)
        instance = null
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}