package com.example.smartguard.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * 🔐 Обнаружение прав суперпользователя (root) на устройстве.
 * Использует многоуровневый анализ для профессиональной защиты.
 */
object RootDetector {

    private const val TAG = "RootDetector"

    /**
     * 🔥 Основной метод проверки. Вызывать при запуске приложения.
     * @return true, если устройство рутовано.
     */
    fun isDeviceRooted(): Boolean {
        return detectRootMethod1() || detectRootMethod2() || detectRootMethod3()
    }

    // 1️⃣ Проверка по наличию бинарных файлов su и magisk
    private fun detectRootMethod1(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su",
            "/magisk/.core/bin/su"  // Magisk
        )
        for (path in paths) {
            if (File(path).exists()) {
                Log.w(TAG, "Root binary found: $path")
                return true
            }
        }
        return false
    }

    // 2️⃣ Проверка системных свойств (ro.build.tags = "test-keys" или ro.debuggable = 1)
    private fun detectRootMethod2(): Boolean {
        return try {
            val buildTags = Build.TAGS
            if (buildTags != null && buildTags.contains("test-keys")) {
                Log.w(TAG, "Test-keys build detected")
                return true
            }
            // Проверка ro.debuggable (для старых версий)
            val process = Runtime.getRuntime().exec("getprop ro.debuggable")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            reader.close()
            line?.contains("1") == true
        } catch (e: Exception) {
            false
        }
    }

    // 3️⃣ Проверка на наличие пакетов приложений для управления root
    private fun detectRootMethod3(): Boolean {
        val rootAppPackages = listOf(
            "com.noshufou.android.su",
            "com.thirdparty.superuser",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.zachspong.temproot",
            "com.ramdroid.appquarantine",
            "com.topjohnwu.magisk"
        )
        // Эта проверка требует доступа к PackageManager, поэтому передадим Context позже
        return false // Будет использоваться в расширенной версии с Context
    }

    /**
     * Расширенная проверка с контекстом (включает сканирование пакетов).
     */
    fun isDeviceRootedWithPackageCheck(context: Context): Boolean {
        if (isDeviceRooted()) return true

        val pm = context.packageManager
        val rootAppPackages = listOf(
            "com.noshufou.android.su",
            "eu.chainfire.supersu",
            "com.topjohnwu.magisk"
        )
        for (pkg in rootAppPackages) {
            try {
                pm.getPackageInfo(pkg, 0)
                Log.w(TAG, "Root management app found: $pkg")
                return true
            } catch (e: PackageManager.NameNotFoundException) {
                // Не найдено — ок
            }
        }
        return false
    }
}