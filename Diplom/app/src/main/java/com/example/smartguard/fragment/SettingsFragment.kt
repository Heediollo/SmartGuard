package com.example.smartguard.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.example.smartguard.R
import com.example.smartguard.security.SecureStorage

class SettingsFragment : Fragment() {

    private lateinit var switchSmsProtection: SwitchCompat
    private lateinit var switchVoiceAnalysis: SwitchCompat
    private lateinit var switchInteractiveAssistant: SwitchCompat
    private lateinit var btnSave: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        switchSmsProtection = view.findViewById(R.id.switchSmsProtection)
        switchVoiceAnalysis = view.findViewById(R.id.switchVoiceAnalysis)
        switchInteractiveAssistant = view.findViewById(R.id.switchInteractiveAssistant)
        btnSave = view.findViewById(R.id.btnSaveSettings)

        loadSettings()

        btnSave.setOnClickListener {
            saveSettings()
            Toast.makeText(requireContext(), "Настройки сохранены", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSettings() {
        val prefs = SecureStorage.getEncryptedPrefs(requireContext())
        switchSmsProtection.isChecked = prefs.getBoolean("sms_protection_enabled", true)
        switchVoiceAnalysis.isChecked = prefs.getBoolean("voice_analysis_enabled", false)
        switchInteractiveAssistant.isChecked = prefs.getBoolean("interactive_assistant_enabled", true)
    }

    private fun saveSettings() {
        val prefs = SecureStorage.getEncryptedPrefs(requireContext())
        prefs.edit()
            .putBoolean("sms_protection_enabled", switchSmsProtection.isChecked)
            .putBoolean("voice_analysis_enabled", switchVoiceAnalysis.isChecked)
            .putBoolean("interactive_assistant_enabled", switchInteractiveAssistant.isChecked)
            .apply()
    }
}