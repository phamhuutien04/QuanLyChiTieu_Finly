package com.example.quanlychitieu_finly

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

// Nếu bạn đã có CatUI ở file riêng, bỏ data class này đi và dùng chung.
data class CatUI(val id: String, val name: String, val iconUrl: String)

class CategoryPickerAdapter(
    private val items: List<CatUI>,
    private val onPick: (CatUI) -> Unit
) : RecyclerView.Adapter<CategoryPickerAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.imgIcon)
        val name: TextView = v.findViewById(R.id.tvName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_choice, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.name.text = item.name

        // Load icon (nếu iconUrl rỗng -> dùng icon mặc định)
        if (item.iconUrl.isBlank()) {
            holder.img.setImageResource(R.drawable.ic_default_category)
        } else {
            Glide.with(holder.itemView)
                .load(item.iconUrl)
                .placeholder(R.drawable.ic_default_category)
                .error(R.drawable.ic_default_category)
                .into(holder.img)
        }

        // ✅ Truyền CatUI vào callback (không truyền View nữa)
        holder.itemView.setOnClickListener {
            val idx = holder.bindingAdapterPosition
            if (idx != RecyclerView.NO_POSITION) {
                onPick(items[idx])
            }
        }
    }

    override fun getItemCount(): Int = items.size
}
