package com.example.smartguard

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.smartguard.call.RealTimeCallService
import com.example.smartguard.database.BlockedNumbersImporter
import com.example.smartguard.ml.ClassifierManager
import com.example.smartguard.security.RootDetector
import com.example.smartguard.security.SecurityAudit
import com.example.smartguard.security.TamperDetector

class SmartGuardApplication : Application() {

    companion object {
        private const val TAG = "SmartGuardApp"

        @Volatile
        var isDeviceRooted = false
            private set

        @Volatile
        var isAppTampered = false
            private set
    }

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "🚀 Application started")

        // 1. Проверка целостности APK (Anti‑Tampering)
        if (performTamperChecks()) {
            return
        }

        // 2. Инициализация AI-классификатора
        try {
            val initialized = ClassifierManager.initialize(this)
            if (initialized) {
                Log.d(TAG, "✅ ClassifierManager initialized successfully")
            } else {
                Log.e(TAG, "❌ Failed to initialize ClassifierManager")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception initializing ClassifierManager", e)
        }

        // 3. Импорт чёрного списка номеров из JSON
        try {
            BlockedNumbersImporter.importFromAssets(this)
            Log.d(TAG, "📥 Blacklist import initiated")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start blacklist import", e)
        }

        // 4. Проверка root-доступа
        checkRootAccess()

        // 5. Запуск сервиса будет выполнен после получения разрешений в MainActivity
        Log.d(TAG, "⏳ Call service will be started after permissions granted")
    }

    private fun performTamperChecks(): Boolean {
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            Log.d(TAG, "⚠️ Debug build – skipping tamper checks")
            return false
        }

        val isSignatureValid = TamperDetector.isAppSignatureValid(this)
        if (!isSignatureValid) {
            Log.e(TAG, "🚨 TAMPER DETECTED! Signature invalid.")
            SecurityAudit.logEvent(
                this,
                SecurityAudit.EventType.TAMPER_DETECTED,
                "Обнаружена модификация APK",
                SecurityAudit.RiskLevel.CRITICAL
            )
            isAppTampered = true
            return true
        }

        Log.d(TAG, "✅ Tamper checks passed. App is genuine.")
        return false
    }

    private fun checkRootAccess() {
        try {
            val rooted = RootDetector.isDeviceRootedWithPackageCheck(this)
            isDeviceRooted = rooted
            if (rooted) {
                Log.w(TAG, "⚠️ ROOT DETECTED! Device is compromised.")
                SecurityAudit.logEvent(
                    this,
                    SecurityAudit.EventType.ROOT_DETECTED,
                    "Обнаружен root-доступ на устройстве. Безопасность снижена.",
                    SecurityAudit.RiskLevel.CRITICAL
                )
            } else {
                Log.d(TAG, "✅ Root not detected. Device is clean.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during root check", e)
            isDeviceRooted = false
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val permissions = listOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.READ_PHONE_STATE
        )
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startCallService() {
        try {
            val intent = Intent(this, RealTimeCallService::class.java).apply {
                action = RealTimeCallService.ACTION_INIT
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, intent)
            } else {
                startService(intent)
            }
            Log.d(TAG, "✅ RealTimeCallService started from Application")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start RealTimeCallService", e)
        }
    }

    fun restartCallServiceIfNeeded() {
        if (hasRequiredPermissions()) {
            startCallService()
        }
    }

    fun getClassifierManager(): ClassifierManager = ClassifierManager
}