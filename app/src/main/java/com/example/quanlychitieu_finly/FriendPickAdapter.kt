package com.example.quanlychitieu_finly

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class FriendPickerAdapter(
    private val data: List<User>,
    currentSelected: List<User>,
) : RecyclerView.Adapter<FriendPickerAdapter.Holder>() {

    // Danh sách id đang được chọn
    private val selectedIds = currentSelected.map { it.id }.toMutableSet()

    inner class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val imgAvatar: ImageView = v.findViewById(R.id.imgAvatar)
        val tvName: TextView = v.findViewById(R.id.tvName)
        val imgCheck: ImageView = v.findViewById(R.id.imgCheck)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_pick, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val u = data[position]

        holder.tvName.text = u.username.ifBlank { u.email }

        Glide.with(holder.imgAvatar.context)
            .load(u.avatarUrl)
            .circleCrop()
            .into(holder.imgAvatar)

        // Hiển thị dấu tick nếu user đã chọn
        val isSelected = selectedIds.contains(u.id)
        holder.imgCheck.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE

        holder.itemView.setOnClickListener {
            if (isSelected) selectedIds.remove(u.id)
            else selectedIds.add(u.id)

            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = data.size

    /** Trả về danh sách User đã chọn */
    fun getSelectedList(): List<User> =
        data.filter { selectedIds.contains(it.id) }
}
