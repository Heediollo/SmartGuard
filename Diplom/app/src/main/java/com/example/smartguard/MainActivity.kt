package com.example.smartguard

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.example.smartguard.call.RealTimeCallService
import com.example.smartguard.databinding.ActivityMainBinding
import com.example.smartguard.fragment.BlockedCallsFragment
import com.example.smartguard.fragment.HomeFragment
import com.example.smartguard.fragment.LinkCheckerFragment
import com.example.smartguard.fragment.SettingsFragment
import com.example.smartguard.fragment.StatisticsFragment
import com.example.smartguard.fragment.TrainingFragment
import com.example.smartguard.ml.ClassifierManager
import com.example.smartguard.security.RootWarningDialog
import com.example.smartguard.security.SecurityAudit
import com.example.smartguard.security.TamperWarningDialog
import com.example.smartguard.ui.HistoryActivity
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle

    companion object {
        private const val TAG = "MainActivity"
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            Log.d(TAG, "✅ SYSTEM_ALERT_WINDOW granted!")
            Toast.makeText(this, "✅ Разрешение на оверлей получено!", Toast.LENGTH_SHORT).show()
            startCallAnalysisService()
        } else {
            Log.w(TAG, "❌ SYSTEM_ALERT_WINDOW denied")
            Toast.makeText(this, "⚠️ Разрешение не выдано", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            Log.d(TAG, "✅ Все разрешения предоставлены")
            Toast.makeText(this, "✅ Все разрешения предоставлены!", Toast.LENGTH_SHORT).show()
            startCallAnalysisService()
        } else {
            Log.w(TAG, "⚠️ Некоторые разрешения отклонены: ${permissions.filter { !it.value }.keys}")
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // === ИНИЦИАЛИЗАЦИЯ AI-КЛАССИФИКАТОРА ===
        try {
            val classifier = ClassifierManager.getClassifier()
            if (classifier != null && classifier.isReady()) {
                Log.d(TAG, "✅ AI-классификатор готов к работе (из singleton)")
                Toast.makeText(this, "🔒 SmartGuard защитник активен", Toast.LENGTH_SHORT).show()
            } else {
                Log.w(TAG, "⚠️ AI-классификатор ещё инициализируется...")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка при получении классификатора", e)
        }

        // Настройка Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "SmartGuard"

        // Настройка Drawer Toggle
        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Настройка Navigation View
        binding.navigationView.setNavigationItemSelectedListener(this)

        // Загружаем главный фрагмент
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
            binding.navigationView.menu.findItem(R.id.nav_home).isChecked = true
        }

        // 🔥 ПРОВЕРКИ БЕЗОПАСНОСТИ
        performSecurityChecks()

        // Проверка, активирован ли CallScreeningService
        checkCallScreeningEnabled()

        // Запрос разрешений
        checkAndRequestPermissions()

        // Запуск анализа, если разрешения уже есть
        if (hasRequiredPermissions()) {
            startCallAnalysisService()
        }

        // Обработка кнопки "Назад"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Логируем запуск
        SecurityAudit.logEvent(
            this,
            SecurityAudit.EventType.USER_ACTION,
            "Приложение запущено",
            SecurityAudit.RiskLevel.LOW
        )
    }

    private fun performSecurityChecks() {
        // 1. Проверка целостности APK (Anti‑Tampering)
        if (SmartGuardApplication.isAppTampered) {
            Log.e(TAG, "🚨 TAMPER DETECTED! Showing warning and exiting.")
            TamperWarningDialog.show(this)
            return
        }

        // 2. Проверка root-доступа
        if (SmartGuardApplication.isDeviceRooted) {
            Log.w(TAG, "⚠️ Root detected! Showing warning dialog.")
            RootWarningDialog.show(this)
        }
    }

    /**
     * Проверяет, активирован ли CallScreeningService для нашего приложения.
     * Используется рефлексия для совместимости с разными compileSdk.
     */
    @SuppressLint("PrivateApi", "DiscouragedPrivateApi")
    private fun checkCallScreeningEnabled() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return // На Android 9 и ниже CallScreeningService не работает
        }

        val telecomManager = getSystemService(TelecomManager::class.java)
        var isOurAppActive = false

        try {
            val method = TelecomManager::class.java.getMethod("getCallScreeningApps")
            val screeningApps = method.invoke(telecomManager) as? List<*>
            screeningApps?.forEach { app ->
                val packageNameField = app?.javaClass?.getField("packageName")
                val pkg = packageNameField?.get(app) as? String
                if (pkg == packageName) {
                    isOurAppActive = true
                    return@forEach
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Не удалось проверить статус CallScreeningService: ${e.message}")
        }

        if (!isOurAppActive) {
            AlertDialog.Builder(this)
                .setTitle("📞 Включите защиту от спама")
                .setMessage(
                    "Для автоматической блокировки мошеннических звонков необходимо " +
                            "включить SmartGuard как определитель номера.\n\n" +
                            "Перейдите в Настройки → Приложения → Приложения по умолчанию → " +
                            "Определитель номера и спама → выберите SmartGuard."
                )
                .setPositiveButton("Открыть настройки") { _, _ ->
                    val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                    startActivity(intent)
                }
                .setNegativeButton("Позже", null)
                .show()
        } else {
            Log.d(TAG, "✅ CallScreeningService активирован")
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val permissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE
        )
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startCallAnalysisService() {
        try {
            val intent = Intent(this, RealTimeCallService::class.java).apply {
                action = RealTimeCallService.ACTION_START_ANALYSIS
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, intent)
            } else {
                startService(intent)
            }

            Log.d(TAG, "✅ Sent START_ANALYSIS command to service")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending command: ${e.message}", e)
            Toast.makeText(this, "⚠️ Не удалось запустить анализ", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        val requiredPermissions = listOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.ANSWER_PHONE_CALLS   // <-- ДОБАВЛЕНО ДЛЯ ОТКЛОНЕНИЯ ЗВОНКОВ
        )

        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (!Settings.canDrawOverlays(this)) {
            Log.d(TAG, "⚠️ SYSTEM_ALERT_WINDOW not granted — requesting")
            showOverlayPermissionHint()
        }

        if (permissionsToRequest.isNotEmpty()) {
            Log.d(TAG, "🔐 Запрашиваем разрешения: ${permissionsToRequest.joinToString(", ")}")
            showPermissionExplanationDialog(permissionsToRequest.toTypedArray())
        } else {
            Log.d(TAG, "✅ Все стандартные разрешения уже предоставлены")
        }
    }

    private fun showPermissionExplanationDialog(permissions: Array<String>) {
        AlertDialog.Builder(this)
            .setTitle("🔐 Необходимы разрешения")
            .setMessage(
                "Для работы защиты от мошенничества SmartGuard нужны:\n\n" +
                        "🎤 Микрофон — анализ разговора в реальном времени\n" +
                        "📞 Телефон — отслеживание входящих звонков\n" +
                        "📨 SMS — проверка сообщений на фишинг\n" +
                        "🔔 Уведомления — мгновенные предупреждения об угрозах\n" +
                        "📵 Ответ на звонки — для автоматического отклонения мошенников\n\n" +
                        "Все данные обрабатываются локально на вашем устройстве."
            )
            .setPositiveButton("Разрешить") { dialog, _ ->
                dialog.dismiss()
                requestPermissionLauncher.launch(permissions)
            }
            .setNegativeButton("Позже") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(
                    this,
                    "⚠️ Приложение будет работать в ограниченном режиме",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun showOverlayPermissionHint() {
        Log.d(TAG, "⚠️ SYSTEM_ALERT_WINDOW not granted — requesting...")
        AlertDialog.Builder(this)
            .setTitle("🔔 Требуется разрешение")
            .setMessage(
                "Для показа предупреждений поверх экрана звонка необходимо разрешение:\n\n" +
                        "📱 «Показывать поверх других приложений»\n\n" +
                        "Без этого подсказки не будут отображаться во время разговора."
            )
            .setPositiveButton("Открыть настройки") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
            }
            .setNegativeButton("Позже") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(
                    this,
                    "⚠️ Подсказки не будут работать без разрешения",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Разрешения не предоставлены")
            .setMessage(
                "Некоторые разрешения не были предоставлены. " +
                        "Приложение может работать некорректно.\n\n" +
                        "Вы можете предоставить их в настройках."
            )
            .setPositiveButton("OK") { _, _ -> }
            .setNeutralButton("Настройки") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .show()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                loadFragment(HomeFragment())
                supportActionBar?.title = "SmartGuard"
            }
            R.id.nav_blacklist -> {
                loadFragment(BlockedCallsFragment())
                supportActionBar?.title = "Чёрный список"
            }
            R.id.nav_statistics -> {
                loadFragment(StatisticsFragment())
                supportActionBar?.title = "Статистика"
            }
            R.id.nav_audit -> {
                val intent = Intent(this, HistoryActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_link_checker -> {
                loadFragment(LinkCheckerFragment())
                supportActionBar?.title = "Проверка ссылок"
            }
            R.id.nav_settings -> {
                loadFragment(SettingsFragment())
                supportActionBar?.title = "Настройки"
            }
            R.id.nav_training -> {
                loadFragment(TrainingFragment())
                supportActionBar?.title = "Обучение"
            }
            R.id.nav_about -> {
                showAboutDialog()
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("О приложении SmartGuard")
            .setMessage(
                "SmartGuard v1.0\n\n" +
                        "Разработано в рамках дипломного проекта\n" +
                        "по специальности «Информационная безопасность».\n\n" +
                        "Приложение защищает от телефонного и SMS-мошенничества с помощью:\n" +
                        "• Искусственного интеллекта (ML)\n" +
                        "• Чёрного списка номеров\n" +
                        "• Интерактивного ассистента\n\n" +
                        "© 2026, Ваше Имя"
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        if (hasRequiredPermissions()) {
            startCallAnalysisService()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    fun openDrawer() {
        binding.drawerLayout.openDrawer(GravityCompat.START)
    }
}