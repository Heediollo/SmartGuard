package com.example.smartguard.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartguard.R
import com.example.smartguard.database.BlockedNumbersDatabase
import com.example.smartguard.security.SecurityAudit
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatisticsFragment : Fragment() {

    private lateinit var tvBlockedCalls: TextView
    private lateinit var tvCheckedNumbers: TextView
    private lateinit var pieChart: PieChart
    private lateinit var lineChart: LineChart
    private lateinit var rvRecentChecks: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvBlockedCalls = view.findViewById(R.id.tvBlockedCalls)
        tvCheckedNumbers = view.findViewById(R.id.tvCheckedNumbers)
        pieChart = view.findViewById(R.id.pieChart)
        lineChart = view.findViewById(R.id.lineChart)
        rvRecentChecks = view.findViewById(R.id.rvRecentChecks)

        rvRecentChecks.layoutManager = LinearLayoutManager(requireContext())

        loadStatistics()
    }

    private fun loadStatistics() {
        lifecycleScope.launch {
            val db = BlockedNumbersDatabase.getDatabase(requireContext())
            val blockedNumbers = withContext(Dispatchers.IO) {
                db.blockedNumberDao().getAllBlockedNumbersOnce()
            }
            val blockedCount = blockedNumbers.size

            // Статистика по категориям (из категорий номеров)
            val categoryCount = blockedNumbers.groupingBy { it.category ?: "Прочее" }.eachCount()

            // Последние проверки (из аудита)
            val recentChecks = SecurityAudit.getAllEvents()
                .filter { it.eventType == SecurityAudit.EventType.USER_ACTION && it.description.contains("Проверка номера") }
                .take(5)

            withContext(Dispatchers.Main) {
                tvBlockedCalls.text = blockedCount.toString()
                tvCheckedNumbers.text = recentChecks.size.toString()

                setupPieChart(categoryCount)
                setupLineChart()
                rvRecentChecks.adapter = RecentChecksAdapter(recentChecks)
            }
        }
    }

    private fun setupPieChart(categoryCount: Map<String, Int>) {
        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()

        categoryCount.forEach { (category, count) ->
            entries.add(PieEntry(count.toFloat(), category))
            colors.add(when (category) {
                "fraud" -> Color.parseColor("#D32F2F")
                "spam" -> Color.parseColor("#FF9800")
                "collector" -> Color.parseColor("#9C27B0")
                else -> Color.parseColor("#757575")
            })
        }

        val dataSet = PieDataSet(entries, "Категории").apply {
            this.colors = colors
            valueTextSize = 14f
            valueTextColor = Color.WHITE
        }
        pieChart.data = PieData(dataSet)
        pieChart.description.isEnabled = false
        pieChart.centerText = "Угрозы"
        pieChart.animateY(1000)
        pieChart.invalidate()
    }

    private fun setupLineChart() {
        // Заглушка – можно заменить на реальные данные из аудита по дням
        val days = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        val entries = listOf(2, 5, 3, 8, 4, 6, 1).mapIndexed { index, value ->
            Entry(index.toFloat(), value.toFloat())
        }

        val dataSet = LineDataSet(entries, "Заблокировано звонков").apply {
            color = Color.parseColor("#1976D2")
            setCircleColor(Color.parseColor("#1976D2"))
            lineWidth = 3f
            circleRadius = 5f
            valueTextSize = 12f
        }

        lineChart.data = LineData(dataSet)
        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(days)
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.xAxis.granularity = 1f
        lineChart.description.isEnabled = false
        lineChart.axisRight.isEnabled = false
        lineChart.animateX(1000)
        lineChart.invalidate()
    }

    inner class RecentChecksAdapter(private val events: List<com.example.smartguard.security.SecurityEvent>) :
        RecyclerView.Adapter<RecentChecksAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvPhone: TextView = view.findViewById(R.id.tvPhone)
            val tvTime: TextView = view.findViewById(R.id.tvTime)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recent_check, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val event = events[position]
            holder.tvPhone.text = event.phoneNumber.ifEmpty { "Неизвестный номер" }
            holder.tvTime.text = event.timestamp
        }

        override fun getItemCount(): Int = events.size
    }
}