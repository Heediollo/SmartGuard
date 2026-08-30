package com.example.smartguard.security

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.security.MessageDigest

object TamperDetector {

    private const val TAG = "TamperDetector"

    // 🔥 ВАШ РЕАЛЬНЫЙ SHA-256 хэш (релизный сертификат)
    private const val EXPECTED_SIGNATURE = "44:27:ED:E8:28:66:91:17:80:8B:B4:02:6B:9F:29:6D:32:BD:A2:24:AF:6C:3C:41:DA:86:91:1B:23:96:5E:BC"

    fun isAppSignatureValid(context: Context): Boolean {
        return try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures.isNullOrEmpty()) {
                Log.e(TAG, "❌ No signatures found!")
                return false
            }

            for (signature in signatures) {
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(signature.toByteArray())
                val hash = digest.joinToString(":") { "%02X".format(it) }

                Log.d(TAG, "Found signature: $hash")
                if (hash == EXPECTED_SIGNATURE) {
                    Log.d(TAG, "✅ Signature matches expected!")
                    return true
                }
            }

            Log.e(TAG, "❌ Signature does not match expected!")
            false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error verifying signature: ${e.message}", e)
            false
        }
    }

    fun isDebugable(context: Context): Boolean {
        return (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
}