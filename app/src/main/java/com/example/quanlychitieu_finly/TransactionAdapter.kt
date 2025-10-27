//
//package com.example.quanlychitieu_finly
//
//import android.graphics.Color
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.FrameLayout
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//import java.text.NumberFormat
//import java.text.SimpleDateFormat
//import java.util.*
//
//import com.bumptech.glide.Glide
//import androidx.core.widget.ImageViewCompat
//import android.content.res.ColorStateList
//
//class TransactionAdapter(private val transactions: List<Transaction>) :
//    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {
//
//    // Fallback màu nếu không có color từ category
//    private val fallbackColors = mapOf(
//        "Ăn uống" to "#FF6B6B",
//        "Di chuyển" to "#4ECDC4",
//        "Mua sắm" to "#95E1D3",
//        "Giải trí" to "#F38181",
//        "Học tập" to "#AA96DA",
//        "Y tế" to "#FCBAD3",
//        "Hóa đơn" to "#A8D8EA",
//        "Lương" to "#4CAF50",
//        "Thưởng" to "#81C784",
//        "Đầu tư" to "#FFD54F",
//        "Khác" to "#B0BEC5"
//    )
//
//    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
//        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
//        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
//        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
//        val iconBackground: FrameLayout = itemView.findViewById(R.id.iconBackground)
//        val ivCategoryIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_transaction, parent, false)
//        return TransactionViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
//        val t = transactions[position]
//
//        holder.tvTitle.text = t.title
//        holder.tvCategory.text = t.categoryName
//
//        val nf = NumberFormat.getInstance(Locale("vi", "VN"))
//        val sign = if (t.type == "income") "+" else "-"
//        holder.tvAmount.text = "$sign${nf.format(t.amount)} đ"
//        holder.tvAmount.setTextColor(Color.parseColor(if (t.type == "income") "#34C759" else "#FF3B30"))
//
//        val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
//        holder.tvDate.text = df.format(t.date.toDate())
//
//        // --- MÀU NỀN HUY HIỆU ---
//        val colorHex = if (t.categoryColorHex.isNotBlank()) t.categoryColorHex
//        else fallbackColors[t.categoryName] ?: "#B0BEC5"
//        holder.iconBackground.setBackgroundColor(Color.parseColor(colorHex))
//
//        // --- ICON TỪ DANH MỤC ---
//        // Quan trọng: bỏ tint để icon không bị trắng hết
//        ImageViewCompat.setImageTintList(holder.ivCategoryIcon, null)
//
//        val urlOrName = t.categoryIconUrl.trim()
//        if (urlOrName.startsWith("http", true)) {
//            Glide.with(holder.itemView.context)
//                .load(urlOrName)
//                .placeholder(android.R.drawable.ic_menu_gallery)
//                .error(android.R.drawable.ic_menu_report_image)
//                .into(holder.ivCategoryIcon)
//        } else {
//            // nếu iconUrl là tên resource nội bộ, ví dụ "ic_food"
//            val resId = holder.itemView.context.resources
//                .getIdentifier(urlOrName, "drawable", holder.itemView.context.packageName)
//            if (resId != 0) holder.ivCategoryIcon.setImageResource(resId)
//            else holder.ivCategoryIcon.setImageResource(android.R.drawable.ic_menu_more)
//        }
//
//        holder.itemView.alpha = 0f
//        holder.itemView.animate().alpha(1f).setDuration(300).setStartDelay((position * 50).toLong()).start()
//    }
//
//    override fun getItemCount(): Int = transactions.size
//}



package com.example.quanlychitieu_finly

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(private val transactions: List<Transaction>) :
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    // Fallback màu nếu transaction.categoryColorHex rỗng
    private val fallbackColors = mapOf(
        "Ăn uống" to "#FF6B6B",
        "Di chuyển" to "#4ECDC4",
        "Mua sắm" to "#95E1D3",
        "Giải trí" to "#F38181",
        "Học tập" to "#AA96DA",
        "Y tế" to "#FCBAD3",
        "Hóa đơn" to "#A8D8EA",
        "Lương" to "#4CAF50",
        "Thưởng" to "#81C784",
        "Đầu tư" to "#FFD54F",
        "Khác" to "#B0BEC5"
    )

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val iconBackground: FrameLayout = itemView.findViewById(R.id.iconBackground)
        val ivCategoryIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val t = transactions[position]

        holder.tvTitle.text = t.title
        holder.tvCategory.text = t.categoryName

        val nf = NumberFormat.getInstance(Locale("vi", "VN"))
        val isIncome = t.type.equals("income", ignoreCase = true)
        val sign = if (isIncome) "+" else "-"
        holder.tvAmount.text = "$sign${nf.format(t.amount)} đ"
        holder.tvAmount.setTextColor(Color.parseColor(if (isIncome) "#34C759" else "#FF3B30"))

        val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        holder.tvDate.text = df.format(t.date.toDate())

        // --- NỀN HUY HIỆU ---
        val colorHex = fallbackColors[t.categoryName] ?: "#B0BEC5"
        holder.iconBackground.setBackgroundColor(Color.parseColor(colorHex))

        // --- ICON ---
        ImageViewCompat.setImageTintList(holder.ivCategoryIcon, null) // bỏ tint

        val urlOrName = t.categoryIconUrl.trim()
        if (urlOrName.startsWith("http", ignoreCase = true)) {
            Glide.with(holder.itemView.context)
                .load(urlOrName)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(holder.ivCategoryIcon)
        } else {
            val safeName = urlOrName.lowercase(Locale.ROOT)
                .replace("-", "_")
                .replace(" ", "_")
            val resId = holder.itemView.context.resources
                .getIdentifier(safeName, "drawable", holder.itemView.context.packageName)
            if (resId != 0) holder.ivCategoryIcon.setImageResource(resId)
            else holder.ivCategoryIcon.setImageResource(android.R.drawable.ic_menu_more)
        }

        holder.itemView.alpha = 0f
        holder.itemView.animate()
            .alpha(1f)
            .setDuration(300)
            .setStartDelay((position * 50).toLong())
            .start()
    }

    override fun getItemCount(): Int = transactions.size
}

