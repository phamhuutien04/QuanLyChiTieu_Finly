package com.example.quanlychitieu_finly

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Modern Expense Detail Adapter - Hiển thị danh sách chi tiêu với thiết kế hiện đại
 */
class ModernExpenseDetailAdapter(
    private val expenseList: List<ExpenseDetail>
) : RecyclerView.Adapter<ModernExpenseDetailAdapter.ExpenseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_modern_expense_detail, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(expenseList[position])
    }

    override fun getItemCount(): Int = expenseList.size

    class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconImageView: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
        private val categoryTextView: TextView = itemView.findViewById(R.id.tvCategoryName)
        private val amountTextView: TextView = itemView.findViewById(R.id.tvAmount)
        private val trendTextView: TextView = itemView.findViewById(R.id.tvTrend)
        private val percentageTextView: TextView = itemView.findViewById(R.id.tvPercentage)

        fun bind(expense: ExpenseDetail) {
            iconImageView.setImageResource(expense.iconRes)
            categoryTextView.text = expense.category
            amountTextView.text = expense.amount

            // Parse trend data
            val isIncrease = expense.trend.startsWith("+")
            val trendValue = expense.trend.replace("+", "").replace("-", "")

            if (isIncrease) {
                trendTextView.text = "↗"
                trendTextView.setTextColor(Color.parseColor("#EF4444"))
                percentageTextView.text = "+$trendValue"
                percentageTextView.setTextColor(Color.parseColor("#EF4444"))
            } else {
                trendTextView.text = "↘"
                trendTextView.setTextColor(Color.parseColor("#10B981"))
                percentageTextView.text = "-$trendValue"
                percentageTextView.setTextColor(Color.parseColor("#10B981"))
            }
        }
    }
}