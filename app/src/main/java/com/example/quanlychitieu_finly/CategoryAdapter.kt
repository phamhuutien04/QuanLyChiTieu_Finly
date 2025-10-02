package com.example.quanlychitieu_finly

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(
    private var list: List<Category>,
    private val onItemClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private var isSpendingTab: Boolean = true  // mặc định tab Chi tiêu

    fun setTab(isSpending: Boolean) {
        this.isSpendingTab = isSpending
        notifyDataSetChanged()
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.imgIcon)
        val name: TextView = itemView.findViewById(R.id.tvTitle)
        val amount: TextView = itemView.findViewById(R.id.tvAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = list[position]
        holder.icon.setImageResource(category.icon)
        holder.name.text = category.title
        holder.amount.text = category.amount

        // Đổi màu số tiền theo tab
        if (isSpendingTab) {
            holder.amount.setTextColor(Color.parseColor("#FF0000")) // đỏ cho Chi tiêu
        } else {
            holder.amount.setTextColor(Color.parseColor("#4CAF50")) // xanh cho Thu nhập
        }

        holder.itemView.setOnClickListener {
            onItemClick(category)
        }
    }

    override fun getItemCount(): Int = list.size
}
