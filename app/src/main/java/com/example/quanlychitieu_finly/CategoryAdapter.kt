package com.example.quanlychitieu_finly

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class CategoryAdapter(
    private var list: List<Category>,
    private val onClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    private var isSpendingTab = true

    fun setTab(isSpending: Boolean) {
        isSpendingTab = isSpending
        notifyDataSetChanged()
    }

    fun updateData(newList: List<Category>) {
        list = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = list[position]
        holder.tvName.text = category.name
        holder.tvAmount.text = "%,dđ".format(category.totalAmount)
        holder.tvName.setTextColor(if (isSpendingTab) Color.RED else Color.GREEN)

        // Hiển thị ảnh
        Glide.with(holder.itemView.context)
            .load(category.iconUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .into(holder.ivIcon)

        holder.itemView.setOnClickListener { onClick(category) }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
        val tvName: TextView = itemView.findViewById(R.id.tvCategoryName)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
    }
}
