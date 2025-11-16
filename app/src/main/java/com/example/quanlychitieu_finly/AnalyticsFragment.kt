package com.example.quanlychitieu_finly

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Modern Analytics Fragment - Quản lý chi tiêu với dữ liệu thật từ Firestore
 */
class AnalyticsFragment : Fragment() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

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
    private lateinit var tvGoalManagementTitle: TextView

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
    private val numberFormat: NumberFormat
        get() = NumberFormat.getInstance(Locale("vi", "VN"))
    private var currentPeriod = Period.DAY

    // Real data variables
    private var totalIncome = 0L
    private var totalExpense = 0L
    private var totalBalance = 0L
    private var categoryExpenses = mutableMapOf<String, CategoryData>()
    private var dailyTransactions = mutableListOf<DailyTransaction>()

    enum class Period {
        DAY, MONTH, YEAR
    }

    data class CategoryData(
        val name: String,
        val amount: Long,
        val iconUrl: String,
        val colorHex: String
    )

    data class DailyTransaction(
        val date: Date,
        val amount: Long
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_analytics, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        initViews(view)
        setupClickListeners()
        loadRealData()

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

        // TextView "Quản lý mục tiêu" - Thêm dòng này
        tvGoalManagementTitle = view.findViewById(R.id.tvGoalManagementTitle)

        // Set default period
        selectPeriod(Period.MONTH)
    }

    private fun setupClickListeners() {
        btnDay.setOnClickListener { selectPeriod(Period.DAY) }
        btnMonth.setOnClickListener { selectPeriod(Period.MONTH) }
        btnYear.setOnClickListener { selectPeriod(Period.YEAR) }

        cardGoalProgress.setOnClickListener { showGoalProgressDetail() }
        cardGoalManagement.setOnClickListener { showGoalManagement() }

        // QUAN TRỌNG: Bấm vào TextView "Quản lý mục tiêu" để mở dialog
        tvGoalManagementTitle.setOnClickListener {
            // Hiệu ứng khi bấm
            it.animate()
                .alpha(0.5f)
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
                .start()

            // Mở dialog quản lý mục tiêu
            showGoalManagement()
        }

        btnViewAll.setOnClickListener { showAllTransactions() }
        fabQuickAdd.setOnClickListener { showQuickAddDialog() }
    }

    private fun loadRealData() {
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(context, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show()
            return
        }

        // Load transactions based on current period
        loadTransactionsFromFirestore(userId)

        // Load goals
        loadGoalsFromFirestore(userId)
    }

    private fun loadGoalsFromFirestore(userId: String) {
        db.collection("users").document(userId)
            .collection("settings")
            .document("goals")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val dailyGoal = document.getLong("dailyGoal") ?: 1500000L
                    val monthlyGoal = document.getLong("monthlyGoal") ?: 40000000L
                    val yearlyGoal = document.getLong("yearlyGoal") ?: 500000000L

                    tvDailyGoal.text = formatCurrency(dailyGoal)
                    tvMonthlyGoal.text = formatCurrency(monthlyGoal)
                    tvYearlyGoal.text = formatCurrency(yearlyGoal)
                }
            }
    }

    private fun loadTransactionsFromFirestore(userId: String) {
        val calendar = Calendar.getInstance()
        val startDate = getStartDateForPeriod(calendar, currentPeriod)

        db.collection("users").document(userId)
            .collection("transactions")
            .whereGreaterThanOrEqualTo("date", startDate)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                totalIncome = 0L
                totalExpense = 0L
                categoryExpenses.clear()
                dailyTransactions.clear()

                for (doc in documents) {
                    val type = doc.getString("type") ?: ""
                    val amount = doc.getLong("amount") ?: 0L
                    val categoryName = doc.getString("categoryName") ?: "Khác"
                    val categoryIconUrl = doc.getString("categoryIconUrl") ?: ""
                    val categoryColorHex = doc.getString("categoryColorHex") ?: "#3B82F6"
                    val date = doc.getDate("date") ?: Date()

                    // Calculate totals
                    when (type) {
                        "income" -> totalIncome += amount
                        "spending" -> {
                            totalExpense += amount

                            // Group by category
                            val existing = categoryExpenses[categoryName]
                            if (existing != null) {
                                categoryExpenses[categoryName] = existing.copy(
                                    amount = existing.amount + amount
                                )
                            } else {
                                categoryExpenses[categoryName] = CategoryData(
                                    name = categoryName,
                                    amount = amount,
                                    iconUrl = categoryIconUrl,
                                    colorHex = categoryColorHex
                                )
                            }

                            // Store for trend chart
                            dailyTransactions.add(DailyTransaction(date, amount))
                        }
                    }
                }

                totalBalance = totalIncome - totalExpense

                // Update UI with real data
                updateUIWithRealData()

                // Load categories for additional info
                loadCategoriesInfo(userId)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Lỗi tải dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadCategoriesInfo(userId: String) {
        db.collection("users").document(userId)
            .collection("categories")
            .get()
            .addOnSuccessListener { documents ->
                // Update category data with full info if needed
                for (doc in documents) {
                    val name = doc.getString("name") ?: continue
                    val iconUrl = doc.getString("iconUrl") ?: ""
                    val colorHex = doc.getString("colorAmount") ?: "#3B82F6"

                    categoryExpenses[name]?.let { existing ->
                        categoryExpenses[name] = existing.copy(
                            iconUrl = if (iconUrl.isNotEmpty()) iconUrl else existing.iconUrl,
                            colorHex = colorHex
                        )
                    }
                }

                // Refresh charts with updated category info
                setupPieChart()
                setupRecyclerView()
            }
    }

    private fun updateUIWithRealData() {
        // Animate main stats
        animateCountUp(tvIncome, 0, totalIncome, "đ")
        animateCountUp(tvExpense, 0, totalExpense, "đ")
        animateCountUp(tvBalance, 0, totalBalance, "đ")

        // Calculate goal progress (example: monthly goal of 40M)
        val monthlyGoal = 40000000L
        val goalProgress = if (monthlyGoal > 0) {
            ((totalExpense.toFloat() / monthlyGoal) * 100).coerceAtMost(100f)
        } else 0f

        animateProgressBar(progressGoal, goalProgress.toInt())
        tvGoalProgress.text = "${goalProgress.toInt()}%"
        tvGoalAmount.text = "${formatCurrency(totalExpense)}/${formatCurrency(monthlyGoal)} đ"

        // Update quick stats
        updateQuickStatsFromRealData()

        // Goals (you can make these configurable)
        tvDailyGoal.text = formatCurrency(1500000)
        tvMonthlyGoal.text = formatCurrency(monthlyGoal)
        tvYearlyGoal.text = formatCurrency(500000000)

        // Setup charts with real data
        setupPieChart()
        setupTrendChart(currentPeriod)
        setupRecyclerView()
    }

    private fun updateQuickStatsFromRealData() {
        val calendar = Calendar.getInstance()

        when (currentPeriod) {
            Period.DAY -> {
                // Average per day in current week
                val daysInPeriod = 7
                val avgDaily = if (daysInPeriod > 0) totalExpense / daysInPeriod else 0L
                tvAvgDaily.text = formatCurrency(avgDaily)

                // Find highest day
                val highestDay = dailyTransactions.maxByOrNull { it.amount }?.amount ?: 0L
                tvHighest.text = formatCurrency(highestDay)
            }
            Period.MONTH -> {
                // Average per month in current period
                val monthsInPeriod = 6
                val avgMonthly = if (monthsInPeriod > 0) totalExpense / monthsInPeriod else totalExpense
                tvAvgDaily.text = formatCurrency(avgMonthly)

                tvHighest.text = formatCurrency(totalExpense)
            }
            Period.YEAR -> {
                // Average per year
                val yearsInPeriod = 5
                val avgYearly = if (yearsInPeriod > 0) totalExpense / yearsInPeriod else totalExpense
                tvAvgDaily.text = formatCurrency(avgYearly)

                tvHighest.text = formatCurrency(totalExpense)
            }
        }

        // Calculate trend (compare with previous period - simplified)
        val trendPercent = 8.2f // You can calculate this based on previous period data
        updateTrend(trendPercent, true)
    }

    private fun getStartDateForPeriod(calendar: Calendar, period: Period): Date {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return when (period) {
            Period.DAY -> {
                // Last 7 days
                calendar.add(Calendar.DAY_OF_MONTH, -6)
                calendar.time
            }
            Period.MONTH -> {
                // Last 6 months
                calendar.add(Calendar.MONTH, -5)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.time
            }
            Period.YEAR -> {
                // Last 5 years
                calendar.add(Calendar.YEAR, -4)
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.time
            }
        }
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

        resetPeriodButtons()
        when (period) {
            Period.DAY -> selectPeriodButton(btnDay)
            Period.MONTH -> selectPeriodButton(btnMonth)
            Period.YEAR -> selectPeriodButton(btnYear)
        }

        // Reload data for new period
        loadRealData()
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

    private fun setupPieChart() {
        if (categoryExpenses.isEmpty()) {
            pieChart.visibility = View.GONE
            return
        }
        pieChart.visibility = View.VISIBLE

        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()

        // Sort by amount and get top categories
        val sortedCategories = categoryExpenses.values.sortedByDescending { it.amount }
        val total = sortedCategories.sumOf { it.amount }.toFloat()

        for (category in sortedCategories) {
            val percentage = (category.amount.toFloat() / total) * 100
            entries.add(PieEntry(percentage, category.name))

            // Parse color or use default
            try {
                colors.add(Color.parseColor(category.colorHex))
            } catch (e: Exception) {
                colors.add(Color.parseColor("#3B82F6"))
            }
        }

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

            animateY(1200, Easing.EaseInOutQuad)

            invalidate()
        }
    }

    private fun setupTrendChart(period: Period) {
        val entries = getTrendDataFromTransactions(period)

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

    private fun getTrendDataFromTransactions(period: Period): List<Entry> {
        if (dailyTransactions.isEmpty()) {
            return listOf(Entry(0f, 0f))
        }

        val groupedData = when (period) {
            Period.DAY -> groupByDay(dailyTransactions)
            Period.MONTH -> groupByMonth(dailyTransactions)
            Period.YEAR -> groupByYear(dailyTransactions)
        }

        return groupedData.mapIndexed { index, amount ->
            Entry(index.toFloat(), (amount / 1000).toFloat())
        }
    }

    private fun groupByDay(transactions: List<DailyTransaction>): List<Long> {
        val calendar = Calendar.getInstance()
        val dayTotals = mutableMapOf<Int, Long>()

        for (i in 0..6) {
            dayTotals[i] = 0L
        }

        for (transaction in transactions) {
            calendar.time = transaction.date
            val dayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0
            dayTotals[dayOfWeek] = (dayTotals[dayOfWeek] ?: 0L) + transaction.amount
        }

        return (0..6).map { dayTotals[it] ?: 0L }
    }

    private fun groupByMonth(transactions: List<DailyTransaction>): List<Long> {
        val calendar = Calendar.getInstance()
        val monthTotals = mutableMapOf<Int, Long>()

        for (i in 0..5) {
            monthTotals[i] = 0L
        }

        for (transaction in transactions) {
            calendar.time = transaction.date
            val monthsAgo = calculateMonthsAgo(transaction.date)
            if (monthsAgo in 0..5) {
                monthTotals[5 - monthsAgo] = (monthTotals[5 - monthsAgo] ?: 0L) + transaction.amount
            }
        }

        return (0..5).map { monthTotals[it] ?: 0L }
    }

    private fun groupByYear(transactions: List<DailyTransaction>): List<Long> {
        val calendar = Calendar.getInstance()
        val yearTotals = mutableMapOf<Int, Long>()

        for (i in 0..4) {
            yearTotals[i] = 0L
        }

        for (transaction in transactions) {
            calendar.time = transaction.date
            val yearsAgo = calculateYearsAgo(transaction.date)
            if (yearsAgo in 0..4) {
                yearTotals[4 - yearsAgo] = (yearTotals[4 - yearsAgo] ?: 0L) + transaction.amount
            }
        }

        return (0..4).map { yearTotals[it] ?: 0L }
    }

    private fun calculateMonthsAgo(date: Date): Int {
        val calendar1 = Calendar.getInstance()
        val calendar2 = Calendar.getInstance()
        calendar2.time = date

        val yearDiff = calendar1.get(Calendar.YEAR) - calendar2.get(Calendar.YEAR)
        val monthDiff = calendar1.get(Calendar.MONTH) - calendar2.get(Calendar.MONTH)

        return yearDiff * 12 + monthDiff
    }

    private fun calculateYearsAgo(date: Date): Int {
        val calendar1 = Calendar.getInstance()
        val calendar2 = Calendar.getInstance()
        calendar2.time = date

        return calendar1.get(Calendar.YEAR) - calendar2.get(Calendar.YEAR)
    }

    private fun getXAxisLabels(period: Period): Array<String> {
        return when (period) {
            Period.DAY -> arrayOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
            Period.MONTH -> {
                val labels = mutableListOf<String>()
                val calendar = Calendar.getInstance()
                for (i in 5 downTo 0) {
                    calendar.add(Calendar.MONTH, if (i == 5) -5 else 1)
                    labels.add("T${calendar.get(Calendar.MONTH) + 1}")
                }
                labels.toTypedArray()
            }
            Period.YEAR -> {
                val labels = mutableListOf<String>()
                val calendar = Calendar.getInstance()
                val currentYear = calendar.get(Calendar.YEAR)
                for (i in 4 downTo 0) {
                    labels.add((currentYear - i).toString())
                }
                labels.toTypedArray()
            }
        }
    }

    private fun setupRecyclerView() {
        val details = categoryExpenses.values
            .sortedByDescending { it.amount }
            .take(5)
            .map { category ->
                ExpenseDetail(
                    iconRes = R.drawable.ic_category_food, // Default icon, you can load from URL
                    category = category.name,
                    amount = "${formatCurrency(category.amount)} đ",
                    trend = "+0%" // Calculate trend if you have historical data
                )
            }

        if (details.isEmpty()) {
            rcvDetail.visibility = View.GONE
        } else {
            rcvDetail.visibility = View.VISIBLE
            rcvDetail.layoutManager = LinearLayoutManager(requireContext())
            rcvDetail.adapter = ModernExpenseDetailAdapter(details)
        }
    }

    private fun animateCountUp(textView: TextView, start: Long, end: Long, suffix: String) {
        val animator = ValueAnimator.ofInt(start.toInt(), end.toInt()).apply {
            duration = 1500L
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { animation ->
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
        Toast.makeText(context, "Chi tiết tiến độ mục tiêu", Toast.LENGTH_SHORT).show()
    }

    private fun showGoalManagement() {
        // Mở dialog quản lý mục tiêu
        context?.let { ctx ->
            val dialog = GoalManagementDialog(ctx) {
                // Callback khi lưu mục tiêu thành công
                loadRealData() // Reload data để cập nhật UI
            }
            dialog.show()
        }
    }

    private fun showAllTransactions() {
        // TODO: Navigate to all transactions screen
        Toast.makeText(context, "Xem tất cả giao dịch", Toast.LENGTH_SHORT).show()
    }

    private fun showQuickAddDialog() {
        // TODO: Show quick add transaction dialog
        Toast.makeText(context, "Thêm giao dịch nhanh", Toast.LENGTH_SHORT).show()
    }
}

// Data class
data class ExpenseDetail(
    val iconRes: Int,
    val category: String,
    val amount: String,
    val trend: String
)