# ============================================
# SMARTGUARD - ProGuard Rules
# ============================================

# === 🎙️ VOSK SPEECH RECOGNITION ===
-keep class org.vosk.** { *; }
-keep class com.alphacephei.** { *; }
-dontwarn org.vosk.**
-dontwarn com.alphacephei.**

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# === 🤖 TENSORFLOW LITE ===
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.support.** { *; }
-dontwarn org.tensorflow.**

# === 🧵 KOTLIN COROUTINES ===
-keepattributes *Annotation*
-keepclassmembers class kotlinx.coroutines.** { *; }

# === 🔐 ANDROIDX SECURITY ===
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# === 🌐 OKHTTP ===
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**

# === 📦 JSON ===
-keep class org.json.** { *; }

# === 🎨 MATERIAL DESIGN ===
-keep class com.google.android.material.** { *; }

# === 🛡️ YOUR APP CLASSES ===
-keep class com.example.smartguard.** { *; }

# Keep data classes (особенно для Gson)
-keepclassmembers class com.example.smartguard.** {
    *** get*();
    void set*(***);
}

# ============================================
# 🔥 НОВЫЕ ПРАВИЛА ДЛЯ GSON И СЦЕНАРИЕВ (КРИТИЧНО!)
# ============================================

# Сохраняем сигнатуры дженериков – это уберёт ошибку TypeToken
-keepattributes Signature

# Полностью сохраняем все классы в пакете ai (Scenario, Step, Response)
-keep class com.example.smartguard.ai.** { *; }

# Сохраняем вложенные классы TypeToken (используются GuidanceManager)
-keep class com.example.smartguard.ai.GuidanceManager$* { *; }

# Сохраняем Gson и TypeToken
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Сохраняем классы для базы данных (BlockedNumbersImporter тоже использует Gson)
-keep class com.example.smartguard.database.** { *; }

# ============================================
# === 🔍 ОПТИМИЗАЦИИ ===
# ============================================

# Удаляем логи в релизе (опционально, у вас уже есть)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Обязательные атрибуты
-keepattributes *Annotation*
-keepattributes InnerClasses