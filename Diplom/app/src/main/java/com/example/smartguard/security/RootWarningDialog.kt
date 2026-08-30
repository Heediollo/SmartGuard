package com.example.smartguard.security

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Process

/**
 * Диалог предупреждения о наличии root-прав.
 */
object RootWarningDialog {

    fun show(
        context: Context,
        onContinue: () -> Unit = {},
        onExit: () -> Unit = { Process.killProcess(Process.myPid()) }
    ) {
        AlertDialog.Builder(context)  // 🔥 убрали кастомный стиль
            .setTitle("⚠️ Обнаружен root-доступ")
            .setMessage(
                "Ваше устройство имеет права суперпользователя (root). " +
                        "Это снижает уровень безопасности и позволяет вредоносным программам обходить защиту.\n\n" +
                        "SmartGuard не гарантирует полную защиту на рутованных устройствах.\n\n" +
                        "Рекомендуется использовать устройство без root."
            )
            .setPositiveButton("Всё равно продолжить") { dialog, _ ->
                dialog.dismiss()
                onContinue()
            }
            .setNegativeButton("Выйти из приложения") { dialog, _ ->
                dialog.dismiss()
                onExit()
            }
            .setCancelable(false)
            .show()
    }
}