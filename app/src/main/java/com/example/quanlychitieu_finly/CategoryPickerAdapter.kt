package com.example.quanlychitieu_finly

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView

// Dữ liệu UI cho danh mục
data class CatUI(val id: String, val name: String, val iconUrl: String)

class CategoryPickerAdapter(
    private val items: List<CatUI>,
    private var selectedId: String? = null, // Biến lưu ID item đang chọn
    private val onPick: (CatUI) -> Unit
) : RecyclerView.Adapter<CategoryPickerAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        // Ánh xạ các View trong item_category_choice.xml
        val cardRoot: MaterialCardView = v.findViewById(R.id.cardRoot)
        val img: ImageView = v.findViewById(R.id.imgIcon)
        val name: TextView = v.findViewById(R.id.tvName)
        val rbSelected: ImageView = v.findViewById(R.id.rbSelected)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_choice, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.name.text = item.name

        // --- XỬ LÝ HÌNH ẢNH ---
        // Dùng hình mặc định của hệ thống nếu chưa có icon riêng
        if (item.iconUrl.isBlank()) {
            holder.img.setImageResource(android.R.drawable.ic_menu_gallery)
        } else {
            Glide.with(holder.itemView)
                .load(item.iconUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(holder.img)
        }

        // --- LOGIC UI KHI ĐƯỢC CHỌN ---
        val isSelected = (item.id == selectedId)

        // 1. Đổi trạng thái nút tròn (Radio Button giả)
        holder.rbSelected.isSelected = isSelected

        // 2. Đổi màu viền Card cho đẹp (Premium UI)
        if (isSelected) {
            holder.cardRoot.strokeColor = Color.parseColor("#4CAF50") // Viền Xanh lá
            holder.cardRoot.strokeWidth = 4
            holder.cardRoot.setCardBackgroundColor(Color.parseColor("#353A65")) // Nền sáng hơn
        } else {
            holder.cardRoot.strokeColor = Color.parseColor("#3F3B66") // Viền tối
            holder.cardRoot.strokeWidth = 2
            holder.cardRoot.setCardBackgroundColor(Color.parseColor("#2D325A")) // Nền tối
        }

        // --- SỰ KIỆN CLICK ---
        holder.itemView.setOnClickListener {
            // Cập nhật ID đang chọn
            selectedId = item.id

            // Refresh lại toàn bộ danh sách để cập nhật màu sắc (Xóa chọn cũ, chọn mới)
            notifyDataSetChanged()

            // Gọi callback trả về kết quả
            onPick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}