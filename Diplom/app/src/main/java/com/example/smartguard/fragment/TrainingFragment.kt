package com.example.smartguard.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.smartguard.databinding.FragmentTrainingBinding
import com.example.smartguard.ui.academy.ModuleDetailActivity

class TrainingFragment : Fragment() {

    private var _binding: FragmentTrainingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrainingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardModule1.setOnClickListener {
            startModuleDetailActivity(
                "Мастер паузы",
                "pause_master",
                "Как не дать мошенникам управлять вами"
            )
        }

        binding.cardModule2.setOnClickListener {
            startModuleDetailActivity(
                "Цифровые ключи",
                "digital_keys",
                "Пароли, 2FA и защита от взлома"
            )
        }

        binding.cardModule3.setOnClickListener {
            startModuleDetailActivity(
                "Энциклопедия мошенника",
                "scam_encyclopedia",
                "Каталог схем и сценариев обмана"
            )
        }
    }

    private fun startModuleDetailActivity(title: String, moduleId: String, description: String) {
        val intent = Intent(requireContext(), ModuleDetailActivity::class.java).apply {
            putExtra("module_title", title)
            putExtra("module_id", moduleId)
            putExtra("module_description", description)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}