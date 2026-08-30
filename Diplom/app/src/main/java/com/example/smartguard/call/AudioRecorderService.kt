package com.example.smartguard.call

import android.media.MediaRecorder
import android.os.Environment
import java.io.File
import java.util.Date
import android.content.Context
import android.util.Log
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder.AudioSource
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Улучшенный сервис для записи аудио во время звонка
 * С исправлениями для Android 10+
 */
class AudioRecorderService(context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var audioRecorder: AudioRecord? = null
    private var outputFile: File? = null
    private var isRecording = false
    private var recordThread: Thread? = null
    private var isStopped = false
    private val contextRef: Context = context.applicationContext

    // Используем публичную папку вместо cache
    private val audioDir: File

    init {
        // Создаём папку в Downloads (видима пользователю)
        audioDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "SmartGuard_Recordings"
        )
        if (!audioDir.exists()) {
            audioDir.mkdirs()
            Log.d("AudioRecorder", "📁 Папка создана: ${audioDir.absolutePath}")
        }
    }

    /**
     * Начать запись разговора (улучшенная версия)
     */
    @Suppress("MissingPermission")
    fun startRecording(): Boolean {
        return try {
            stopRecording() // Останавливаем если была старая запись

            isStopped = false

            // Создаём новый файл с меткой времени
            val timestamp = Date().time.toString()
            outputFile = File(audioDir, "smartguard_call_$timestamp.m4a")

            Log.d("AudioRecorder", "📝 Начинаю запись в: ${outputFile?.absolutePath}")

            // Попытка 1: MediaRecorder с Voice Call источником
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(AudioSource.VOICE_CALL) // Пытаемся записать голос из звонка
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setOutputFile(outputFile?.absolutePath)

                prepare()
                start()
                isRecording = true

                Log.d("AudioRecorder", "✅ Запись начата (MediaRecorder)")
            }

            true

        } catch (e1: SecurityException) {
            Log.e("AudioRecorder", "❌ Нет разрешения RECORD_AUDIO: ${e1.message}")
            false
        } catch (e1: Exception) {
            Log.e("AudioRecorder", "❌ MediaRecorder не сработал: ${e1.message}")

            // Попытка 2: AudioRecord (альтернативный метод)
            try {
                startAudioRecordFallback()
                true
            } catch (e2: Exception) {
                Log.e("AudioRecorder", "❌ AudioRecord тоже не сработал: ${e2.message}")
                isRecording = false
                false
            }
        }
    }

    /**
     * Альтернативный метод записи через AudioRecord
     */
    @Suppress("MissingPermission")
    private fun startAudioRecordFallback() {
        val bufferSize = AudioRecord.getMinBufferSize(
            44100,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecorder = AudioRecord(
            AudioSource.MIC,
            44100,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (audioRecorder?.state == AudioRecord.STATE_INITIALIZED) {
            audioRecorder?.startRecording()
            isRecording = true

            // Запускаем поток для записи
            recordThread = Thread {
                val file = outputFile ?: File(audioDir, "smartguard_call_fallback.m4a")
                file.outputStream().use { fos ->
                    val data = ByteArray(bufferSize)
                    while (isRecording && !isStopped) {
                        val read = audioRecorder?.read(data, 0, bufferSize) ?: 0
                        if (read > 0) {
                            fos.write(data, 0, read)
                        }
                    }
                }
            }
            recordThread?.start()

            Log.d("AudioRecorder", "✅ Запись начата (AudioRecord)")
        } else {
            throw RuntimeException("AudioRecord не инициализировался")
        }
    }

    /**
     * Остановить запись и сохранить файл
     */
    fun stopRecording(): String? {
        return try {
            isStopped = true

            // Останавливаем MediaRecorder
            if (mediaRecorder != null) {
                if (isRecording) {
                    try {
                        mediaRecorder?.stop()
                    } catch (e: RuntimeException) {
                        Log.e("AudioRecorder", "⚠️ Ошибка при stop(): ${e.message}")
                    }
                    mediaRecorder?.release()
                }
                mediaRecorder = null
                Log.d("AudioRecorder", "⏹️ MediaRecorder остановлен")
            }

            // Останавливаем AudioRecord
            if (audioRecorder != null) {
                if (isRecording) {
                    audioRecorder?.stop()
                    audioRecorder?.release()
                }
                audioRecorder = null
                Log.d("AudioRecorder", "⏹️ AudioRecord остановлен")
            }

            // Ждём завершения потока записи
            recordThread?.join(1000)
            recordThread = null

            isRecording = false

            // Возвращаем путь к файлу
            val path = outputFile?.absolutePath
            Log.d("AudioRecorder", "💾 Файл сохранён: $path")

            // Проверяем размер файла
            val size = outputFile?.length() ?: 0
            if (size > 0) {
                Log.d("AudioRecorder", "📊 Размер файла: ${size / 1024} KB")
            } else {
                Log.w("AudioRecorder", "⚠️ Файл пустой!")
            }

            path

        } catch (e: SecurityException) {
            Log.e("AudioRecorder", "❌ Нет разрешения при остановке: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e("AudioRecorder", "❌ Ошибка остановки: ${e.message}")
            null
        }
    }

    /**
     * Проверка идёт ли сейчас запись
     */
    fun isRecording(): Boolean = isRecording

    /**
     * Получить список записанных файлов
     */
    fun getRecordings(): List<File> {
        return audioDir.listFiles()?.filter { file ->
            file.name.endsWith(".m4a") || file.name.endsWith(".pcm")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * Очистить все записи (для тестов)
     */
    @Suppress("UNUSED")
    fun clearRecordings() {
        val recordings = getRecordings()
        recordings.forEach { file ->
            if (file.delete()) {
                Log.d("AudioRecorder", "🗑️ Удалено: ${file.name}")
            }
        }
    }
}