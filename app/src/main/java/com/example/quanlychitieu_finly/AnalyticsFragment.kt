package com.example.quanlychitieu_finly

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import java.text.NumberFormat
import java.util.*

/**
 * Modern Analytics Fragment - Quản lý chi tiêu hiện đại & chuyên nghiệp
 */
class AnalyticsFragment : Fragment() {

    // UI Components
    private lateinit var tvIncome: TextView
    private lateinit var tvExpense: TextView
    private lateinit var tvBalance: TextView
    private lateinit var tvGoalProgress: TextView
    private lateinit var tvGoalAmount: TextView
    private lateinit var tvAvgDaily: TextView
    private lateinit var tvHighest: TextView
    private lateinit var tvTrendIcon: TextView
    private lateinit var tvTrendPercent: TextView
    private lateinit var tvDailyGoal: TextView
    private lateinit var tvMonthlyGoal: TextView
    private lateinit var tvYearlyGoal: TextView

    private lateinit var progressGoal: ProgressBar
    private lateinit var pieChart: PieChart
    private lateinit var trendChart: LineChart
    private lateinit var rcvDetail: RecyclerView
    private lateinit var fabQuickAdd: ExtendedFloatingActionButton

    private lateinit var btnDay: TextView
    private lateinit var btnMonth: TextView
    private lateinit var btnYear: TextView
    private lateinit var cardGoalProgress: View
    private lateinit var cardGoalManagement: View
    private lateinit var btnViewAll: TextView

    // Data
    private val numberFormat = NumberFormat.getInstance(Locale("vi", "VN"))
    private var currentPeriod = Period.DAY
    private var currentGoalProgress = 68f

    enum class Period {
        DAY, MONTH, YEAR
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_analytics, container, false)

        initViews(view)
        setupClickListeners()
        loadData()
        setupCharts()
        setupRecyclerView()

        return view
    }

    private fun initViews(view: View) {
        // Main stats
        tvIncome = view.findViewById(R.id.tvIncome)
        tvExpense = view.findViewById(R.id.tvExpense)
        tvBalance = view.findViewById(R.id.tvBalance)

        // Goal tracking
        tvGoalProgress = view.findViewById(R.id.tvGoalProgress)
        tvGoalAmount = view.findViewById(R.id.tvGoalAmount)
        progressGoal = view.findViewById(R.id.progressGoal)

        // Quick stats
        tvAvgDaily = view.findViewById(R.id.tvAvgDaily)
        tvHighest = view.findViewById(R.id.tvHighest)
        tvTrendIcon = view.findViewById(R.id.tvTrendIcon)
        tvTrendPercent = view.findViewById(R.id.tvTrendPercent)

        // Goals
        tvDailyGoal = view.findViewById(R.id.tvDailyGoal)
        tvMonthlyGoal = view.findViewById(R.id.tvMonthlyGoal)
        tvYearlyGoal = view.findViewById(R.id.tvYearlyGoal)

        // Period selectors
        btnDay = view.findViewById(R.id.btnDay)
        btnMonth = view.findViewById(R.id.btnMonth)
        btnYear = view.findViewById(R.id.btnYear)

        // Charts
        pieChart = view.findViewById(R.id.pieChart)
        trendChart = view.findViewById(R.id.trendChart)

        // Lists
        rcvDetail = view.findViewById(R.id.rcvDetail)

        // Interactive elements
        cardGoalProgress = view.findViewById(R.id.cardGoalProgress)
        cardGoalManagement = view.findViewById(R.id.cardGoalManagement)
        btnViewAll = view.findViewById(R.id.btnViewAll)
        fabQuickAdd = view.findViewById(R.id.fabQuickAdd)
    }

    private fun setupClickListeners() {
        // Period selector clicks
        btnDay.setOnClickListener { selectPeriod(Period.DAY) }
        btnMonth.setOnClickListener { selectPeriod(Period.MONTH) }
        btnYear.setOnClickListener { selectPeriod(Period.YEAR) }

        // Interactive cards
        cardGoalProgress.setOnClickListener {
            // Navigate to goal progress detail
            showGoalProgressDetail()
        }

        cardGoalManagement.setOnClickListener {
            // Navigate to goal management
            showGoalManagement()
        }

        btnViewAll.setOnClickListener {
            // Navigate to detailed transactions
            showAllTransactions()
        }

        fabQuickAdd.setOnClickListener {
            // Show quick add transaction dialog
            showQuickAddDialog()
        }
    }

    private fun loadData() {
        // Simulate loading data with animations
        animateCountUp(tvIncome, 0, 41000350, "đ")
        animateCountUp(tvExpense, 0, 28351000, "đ")
        animateCountUp(tvBalance, 0, 12649350, "đ")

        // Update goal progress
        animateProgressBar(progressGoal, currentGoalProgress.toInt())
        tvGoalProgress.text = "${currentGoalProgress.toInt()}%"
        tvGoalAmount.text = "28M/41M đ"

        // Quick stats
        updateQuickStats(currentPeriod)

        // Goals
        tvDailyGoal.text = formatCurrency(1500000)
        tvMonthlyGoal.text = formatCurrency(41000000)
        tvYearlyGoal.text = formatCurrency(500000000)
    }

    private fun updateQuickStats(period: Period) {
        when (period) {
            Period.DAY -> {
                tvAvgDaily.text = formatCurrency(945000)
                tvHighest.text = formatCurrency(2100000)
                updateTrend(8.2f, true)
            }
            Period.MONTH -> {
                tvAvgDaily.text = formatCurrency(28351000)
                tvHighest.text = formatCurrency(35000000)
                updateTrend(12.5f, true)
            }
            Period.YEAR -> {
                tvAvgDaily.text = formatCurrency(340000000)
                tvHighest.text = formatCurrency(380000000)
                updateTrend(5.3f, false)
            }
        }

        // Update trend chart
        setupTrendChart(period)
    }

    private fun updateTrend(percent: Float, isIncrease: Boolean) {
        tvTrendIcon.text = if (isIncrease) "↗" else "↘"
        tvTrendPercent.text = "${percent}%"

        val color = if (isIncrease) "#EF4444" else "#10B981"
        tvTrendIcon.setTextColor(Color.parseColor(color))
        tvTrendPercent.setTextColor(Color.parseColor(color))
    }

    private fun selectPeriod(period: Period) {
        currentPeriod = period

        // Update UI
        resetPeriodButtons()
        when (period) {
            Period.DAY -> selectPeriodButton(btnDay)
            Period.MONTH -> selectPeriodButton(btnMonth)
            Period.YEAR -> selectPeriodButton(btnYear)
        }

        // Update data
        updateQuickStats(period)
    }

    private fun resetPeriodButtons() {
        listOf(btnDay, btnMonth, btnYear).forEach { btn ->
            btn.setBackgroundResource(0)
            btn.setTextColor(Color.parseColor("#6B7280"))
        }
    }

    private fun selectPeriodButton(button: TextView) {
        button.setBackgroundResource(R.drawable.bg_period_selected)
        button.setTextColor(Color.parseColor("#3B82F6"))
    }

    private fun setupCharts() {
        setupPieChart()
        setupTrendChart(currentPeriod)
    }

    private fun setupPieChart() {
        val entries = listOf(
            PieEntry(30f, "Ăn uống"),
            PieEntry(20f, "Giao thông"),
            PieEntry(15f, "Mua sắm"),
            PieEntry(12f, "Giải trí"),
            PieEntry(10f, "Học tập"),
            PieEntry(8f, "Y tế"),
            PieEntry(5f, "Khác")
        )

        val colors = listOf(
            Color.parseColor("#3B82F6"),
            Color.parseColor("#10B981"),
            Color.parseColor("#F59E0B"),
            Color.parseColor("#EF4444"),
            Color.parseColor("#8B5CF6"),
            Color.parseColor("#06B6D4"),
            Color.parseColor("#84CC16")
        )

        val dataSet = PieDataSet(entries, "").apply {
            setColors(colors)
            valueTextColor = Color.WHITE
            valueTextSize = 12f
            sliceSpace = 3f
            selectionShift = 8f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "${value.toInt()}%"
                }
            }
        }

        pieChart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            legend.apply {
                isEnabled = true
                textSize = 12f
                textColor = Color.parseColor("#4B5563")
                xEntrySpace = 8f
                yEntrySpace = 4f
            }

            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 45f
            transparentCircleRadius = 50f

            setUsePercentValues(true)
            setEntryLabelColor(Color.parseColor("#1F2937"))
            setEntryLabelTextSize(10f)

            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true

            animateY(1200, com.github.mikephil.charting.animation.Easing.EaseInOutQuad)

            invalidate()
        }
    }

    private fun setupTrendChart(period: Period) {
        val entries = when (period) {
            Period.DAY -> getDailyTrendData()
            Period.MONTH -> getMonthlyTrendData()
            Period.YEAR -> getYearlyTrendData()
        }

        val dataSet = LineDataSet(entries, "Chi tiêu").apply {
            color = Color.parseColor("#3B82F6")
            setCircleColor(Color.parseColor("#3B82F6"))
            lineWidth = 3f
            circleRadius = 5f
            setDrawCircleHole(false)
            setDrawValues(false)
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.chart_fill_gradient)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        trendChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false

            axisRight.isEnabled = false
            axisLeft.apply {
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textColor = Color.parseColor("#9CA3AF")
                textSize = 10f
            }

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textColor = Color.parseColor("#9CA3AF")
                textSize = 10f

                valueFormatter = IndexAxisValueFormatter(getXAxisLabels(period))
            }

            setTouchEnabled(false)
            isDragEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)

            animateX(1000)

            invalidate()
        }
    }

    private fun getDailyTrendData(): List<Entry> {
        return listOf(
            Entry(0f, 850f),
            Entry(1f, 1200f),
            Entry(2f, 750f),
            Entry(3f, 1800f),
            Entry(4f, 950f),
            Entry(5f, 1100f),
            Entry(6f, 1300f)
        )
    }

    private fun getMonthlyTrendData(): List<Entry> {
        return listOf(
            Entry(0f, 25f),
            Entry(1f, 28f),
            Entry(2f, 32f),
            Entry(3f, 26f),
            Entry(4f, 35f),
            Entry(5f, 28f)
        )
    }

    private fun getYearlyTrendData(): List<Entry> {
        return listOf(
            Entry(0f, 280f),
            Entry(1f, 320f),
            Entry(2f, 340f),
            Entry(3f, 365f),
            Entry(4f, 380f)
        )
    }

    private fun getXAxisLabels(period: Period): Array<String> {
        return when (period) {
            Period.DAY -> arrayOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
            Period.MONTH -> arrayOf("T1", "T2", "T3", "T4", "T5", "T6")
            Period.YEAR -> arrayOf("2021", "2022", "2023", "2024", "2025")
        }
    }

    private fun setupRecyclerView() {
        val details = listOf(
            ExpenseDetail(R.drawable.ic_category_food, "Ăn uống", "8.500.000 đ", "+12%"),
            ExpenseDetail(R.drawable.ic_transport, "Giao thông", "5.600.000 đ", "+5%"),
            ExpenseDetail(R.drawable.ic_category_shopping, "Mua sắm", "4.200.000 đ", "-8%"),
            ExpenseDetail(R.drawable.ic_play, "Giải trí", "3.400.000 đ", "+15%"),
            ExpenseDetail(R.drawable.ic_study, "Học tập", "2.800.000 đ", "+3%")
        )

        rcvDetail.layoutManager = LinearLayoutManager(requireContext())
        rcvDetail.adapter = ModernExpenseDetailAdapter(details)
    }

    private fun animateCountUp(textView: TextView, start: Long, end: Long, suffix: String) {
        // Dùng ofInt, truyền giá trị Int (nếu số quá lớn có thể dùng ofFloat)
        val animator = ValueAnimator.ofInt(start.toInt(), end.toInt()).apply {
            duration = 1500L
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { animation ->
                // Lấy value dạng Int rồi convert về Long nếu muốn định dạng tiếp
                val value = (animation.animatedValue as Int).toLong()
                textView.text = "${formatCurrency(value)} $suffix"
            }
        }
        animator.start()
    }

    private fun animateProgressBar(progressBar: ProgressBar, targetProgress: Int) {
        val animator = ValueAnimator.ofInt(0, targetProgress).apply {
            duration = 1200
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { animation ->
                progressBar.progress = animation.animatedValue as Int
            }
        }
        animator.start()
    }

    private fun formatCurrency(amount: Long): String {
        return when {
            amount >= 1_000_000_000 -> "${(amount / 1_000_000_000.0).let { "%.1f".format(it) }}B"
            amount >= 1_000_000 -> "${(amount / 1_000_000.0).let { "%.1f".format(it) }}M"
            amount >= 1_000 -> "${(amount / 1_000.0).let { "%.1f".format(it) }}K"
            else -> numberFormat.format(amount)
        }
    }

    // Navigation methods
    private fun showGoalProgressDetail() {
        // TODO: Navigate to goal progress detail screen
    }

    private fun showGoalManagement() {
        // TODO: Navigate to goal management screen
    }

    private fun showAllTransactions() {
        // TODO: Navigate to all transactions screen
    }

    private fun showQuickAddDialog() {
        // TODO: Show quick add transaction dialog
    }
}

// Data classes
data class ExpenseDetail(
    val iconRes: Int,
    val category: String,
    val amount: String,
    val trend: String
)