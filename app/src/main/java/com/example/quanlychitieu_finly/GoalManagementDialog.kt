package com.example.quanlychitieu_finly

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.*
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GoalManagementDialog(
    context: Context,
    private val onGoalSaved: () -> Unit
) : Dialog(context) {

    private lateinit var etDailyGoal: TextInputEditText
    private lateinit var etMonthlyGoal: TextInputEditText
    private lateinit var etYearlyGoal: TextInputEditText
    private lateinit var switchNotification: Switch
    private lateinit var seekbarThreshold: SeekBar
    private lateinit var tvThresholdValue: TextView
    private lateinit var btnSaveGoal: Button
    private lateinit var btnCancel: Button
    private lateinit var btnClose: ImageView

    // Preset cards
    private lateinit var cardPresetLow: androidx.cardview.widget.CardView
    private lateinit var cardPresetMedium: androidx.cardview.widget.CardView
    private lateinit var cardPresetHigh: androidx.cardview.widget.CardView
    private lateinit var cardSmartSuggestion: androidx.cardview.widget.CardView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_goal_management)

        // Làm cho dialog có kích thước phù hợp
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.95).toInt(),
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
        )

        initViews()
        setupListeners()
        loadCurrentGoals()
    }

    private fun initViews() {
        etDailyGoal = findViewById(R.id.etDailyGoal)
        etMonthlyGoal = findViewById(R.id.etMonthlyGoal)
        etYearlyGoal = findViewById(R.id.etYearlyGoal)
        switchNotification = findViewById(R.id.switchNotification)
        seekbarThreshold = findViewById(R.id.seekbarThreshold)
        tvThresholdValue = findViewById(R.id.tvThresholdValue)
        btnSaveGoal = findViewById(R.id.btnSaveGoal)
        btnCancel = findViewById(R.id.btnCancel)
        btnClose = findViewById(R.id.btnClose)

        cardPresetLow = findViewById(R.id.cardPresetLow)
        cardPresetMedium = findViewById(R.id.cardPresetMedium)
        cardPresetHigh = findViewById(R.id.cardPresetHigh)
        cardSmartSuggestion = findViewById(R.id.cardSmartSuggestion)
    }

    private fun setupListeners() {
        // Close button
        btnClose.setOnClickListener { dismiss() }
        btnCancel.setOnClickListener { dismiss() }

        // SeekBar for threshold
        seekbarThreshold.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvThresholdValue.text = "$progress%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Preset buttons
        cardPresetLow.setOnClickListener {
            setPresetGoals(500000, 15000000, 180000000) // Tiết kiệm
        }

        cardPresetMedium.setOnClickListener {
            setPresetGoals(1000000, 30000000, 360000000) // Cân bằng
        }

        cardPresetHigh.setOnClickListener {
            setPresetGoals(2000000, 60000000, 720000000) // Thoải mái
        }

        // Smart suggestion based on spending history
        cardSmartSuggestion.setOnClickListener {
            calculateSmartGoals()
        }

        // Save button
        btnSaveGoal.setOnClickListener {
            saveGoals()
        }
    }

    private fun setPresetGoals(daily: Long, monthly: Long, yearly: Long) {
        etDailyGoal.setText(daily.toString())
        etMonthlyGoal.setText(monthly.toString())
        etYearlyGoal.setText(yearly.toString())
    }

    private fun calculateSmartGoals() {
        val userId = auth.currentUser?.uid ?: return

        // Tính toán mục tiêu thông minh dựa trên chi tiêu trung bình
        db.collection("users").document(userId)
            .collection("transactions")
            .whereEqualTo("type", "spending")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(context, "Chưa có dữ liệu chi tiêu", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                var totalSpending = 0L
                var count = 0

                for (doc in documents) {
                    totalSpending += doc.getLong("amount") ?: 0L
                    count++
                }

                // Tính trung bình và giảm 15% để dễ đạt mục tiêu
                val avgSpending = totalSpending / count
                val reducedGoal = (avgSpending * 0.85).toLong()

                // Tính các mục tiêu tương ứng
                val dailyGoal = reducedGoal
                val monthlyGoal = dailyGoal * 30
                val yearlyGoal = monthlyGoal * 12

                setPresetGoals(dailyGoal, monthlyGoal, yearlyGoal)

                Toast.makeText(
                    context,
                    "Đã tính toán mục tiêu thông minh dựa trên chi tiêu của bạn!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadCurrentGoals() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("settings")
            .document("goals")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    etDailyGoal.setText(document.getLong("dailyGoal")?.toString() ?: "")
                    etMonthlyGoal.setText(document.getLong("monthlyGoal")?.toString() ?: "")
                    etYearlyGoal.setText(document.getLong("yearlyGoal")?.toString() ?: "")

                    switchNotification.isChecked = document.getBoolean("notificationEnabled") ?: true

                    val threshold = document.getLong("notificationThreshold")?.toInt() ?: 80
                    seekbarThreshold.progress = threshold
                    tvThresholdValue.text = "$threshold%"
                }
            }
    }

    private fun saveGoals() {
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(context, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show()
            return
        }

        val dailyGoal = etDailyGoal.text.toString().toLongOrNull() ?: 0L
        val monthlyGoal = etMonthlyGoal.text.toString().toLongOrNull() ?: 0L
        val yearlyGoal = etYearlyGoal.text.toString().toLongOrNull() ?: 0L

        if (dailyGoal <= 0 || monthlyGoal <= 0 || yearlyGoal <= 0) {
            Toast.makeText(context, "Vui lòng nhập đầy đủ các mục tiêu!", Toast.LENGTH_SHORT).show()
            return
        }

        val goalData = hashMapOf(
            "dailyGoal" to dailyGoal,
            "monthlyGoal" to monthlyGoal,
            "yearlyGoal" to yearlyGoal,
            "notificationEnabled" to switchNotification.isChecked,
            "notificationThreshold" to seekbarThreshold.progress,
            "updatedAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("users").document(userId)
            .collection("settings")
            .document("goals")
            .set(goalData)
            .addOnSuccessListener {
                Toast.makeText(context, "Đã lưu mục tiêu thành công!", Toast.LENGTH_SHORT).show()
                onGoalSaved()
                dismiss()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Lỗi lưu mục tiêu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}