package com.example.smartguard.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartguard.R
import com.example.smartguard.database.BlockedCallHistory
import com.example.smartguard.database.BlockedNumber
import com.example.smartguard.database.BlockedNumbersDatabase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class BlockedCallsFragment : Fragment() {

    companion object {
        private const val TAG = "BlockedCallsFragment"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var adapter: HistoryAdapter
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_blocked_calls, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewBlockedCalls)
        fabAdd = view.findViewById(R.id.fabAddNumber)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = HistoryAdapter(emptyList())
        recyclerView.adapter = adapter

        fabAdd.setOnClickListener {
            showAddNumberDialog()
        }

        loadHistory()
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            val db = BlockedNumbersDatabase.getDatabase(requireContext())
            val history = withContext(Dispatchers.IO) {
                db.blockedCallHistoryDao().getAllHistory()
            }
            adapter.updateData(history)
        }
    }

    // Такая же нормализация, как в CallScreeningService
    private fun normalizePhoneNumber(number: String): String {
        val cleaned = number.replace("[^+0-9]".toRegex(), "")
        return when {
            cleaned.startsWith("+") -> cleaned
            cleaned.startsWith("8") && cleaned.length == 11 -> "+7" + cleaned.substring(1)
            cleaned.length == 11 && (cleaned.startsWith("7") || cleaned.startsWith("8")) -> "+7" + cleaned.substring(1)
            cleaned.length == 10 -> "+7" + cleaned
            else -> "+$cleaned"
        }
    }

    private fun showAddNumberDialog() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_number, null)
        val etNumber = view.findViewById<EditText>(R.id.etNumber)
        val actCategory = view.findViewById<AutoCompleteTextView>(R.id.actCategory)

        val categories = listOf("fraud", "spam", "collector", "other")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        actCategory.setAdapter(adapter)
        actCategory.setText(categories[0], false)

        AlertDialog.Builder(requireContext())
            .setTitle("Добавить номер в чёрный список")
            .setView(view)
            .setPositiveButton("Добавить") { dialog, _ ->
                val rawNumber = etNumber.text.toString().trim()
                if (rawNumber.isNotBlank()) {
                    val normalized = normalizePhoneNumber(rawNumber)
                    Log.d(TAG, "Добавление номера: исходный='$rawNumber' → нормализованный='$normalized'")
                    addNumberToBlocklist(normalized, actCategory.text.toString())
                } else {
                    Toast.makeText(requireContext(), "Введите номер", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun addNumberToBlocklist(phoneNumber: String, category: String) {
        lifecycleScope.launch {
            val db = BlockedNumbersDatabase.getDatabase(requireContext())
            // Проверяем существование уже по нормализованному номеру
            val existing = withContext(Dispatchers.IO) {
                db.blockedNumberDao().findByNumber(phoneNumber)
            }
            if (existing != null) {
                Toast.makeText(requireContext(), "Номер уже в чёрном списке", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val blockedNumber = BlockedNumber(
                phoneNumber = phoneNumber,
                category = category,
                dateAdded = System.currentTimeMillis()
            )
            withContext(Dispatchers.IO) {
                db.blockedNumberDao().insert(blockedNumber)
            }
            Toast.makeText(requireContext(), "Номер добавлен в чёрный список", Toast.LENGTH_SHORT).show()
        }
    }

    inner class HistoryAdapter(private var historyList: List<BlockedCallHistory>) :
        RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvPhone: android.widget.TextView = view.findViewById(R.id.tvPhoneNumber)
            val tvDate: android.widget.TextView = view.findViewById(R.id.tvDate)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_blocked_call_history, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = historyList[position]
            holder.tvPhone.text = item.phoneNumber
            holder.tvDate.text = dateFormat.format(Date(item.timestamp))
        }

        override fun getItemCount(): Int = historyList.size

        fun updateData(newList: List<BlockedCallHistory>) {
            historyList = newList
            notifyDataSetChanged()
        }
    }
}