package com.example.quanlychitieu_finly

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AnalyticsDetailAdapter(
    private val list: List<FinanceDetail>
) : RecyclerView.Adapter<AnalyticsDetailAdapter.DetailViewHolder>() {

    inner class DetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgIcon: ImageView = itemView.findViewById(R.id.imgIcon)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_analytics_detail, parent, false)
        return DetailViewHolder(v)
    }

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
        val item = list[position]
        holder.imgIcon.setImageResource(item.iconRes)
        holder.tvName.text = item.name
        holder.tvAmount.text = item.amount
    }

    override fun getItemCount(): Int = list.size
}
