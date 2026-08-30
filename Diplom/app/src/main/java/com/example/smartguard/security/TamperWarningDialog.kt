package com.example.smartguard.security

import android.app.AlertDialog
import android.content.Context
import android.os.Process

/**
 * Диалог критического предупреждения при обнаружении модификации APK.
 */
object TamperWarningDialog {

    fun show(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("🚨 КРИТИЧЕСКАЯ ОШИБКА")
            .setMessage(
                "Приложение было модифицировано или переподписано.\n\n" +
                        "Это может означать, что ваша версия SmartGuard была взломана " +
                        "и не может гарантировать безопасность ваших данных.\n\n" +
                        "Приложение будет закрыто."
            )
            .setPositiveButton("Выйти") { dialog, _ ->
                dialog.dismiss()
                Process.killProcess(Process.myPid())
            }
            .setCancelable(false)
            .show()
    }
}