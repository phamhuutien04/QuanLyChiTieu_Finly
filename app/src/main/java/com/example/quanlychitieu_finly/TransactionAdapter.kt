package com.example.quanlychitieu_finly

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TransactionAdapter(private val transactions: List<Transaction>) :
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.tvTitle.text = transaction.title
        holder.tvCategory.text = transaction.category
        holder.tvAmount.text = transaction.amount
        holder.tvDate.text = transaction.date

        // Nếu chi tiêu thì đỏ, thu nhập thì xanh
        if (transaction.amount.contains("-")) {
            holder.tvAmount.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
        } else {
            holder.tvAmount.setTextColor(holder.itemView.context.getColor(android.R.color.holo_green_dark))
        }
    }

    override fun getItemCount(): Int = transactions.size
}
