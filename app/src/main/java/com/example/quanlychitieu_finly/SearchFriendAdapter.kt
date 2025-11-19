package com.example.quanlychitieu_finly

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class SearchFriendAdapter(
    private var list: MutableList<User>,
    private val onAddFriend: (User) -> Unit,
    private val onOpenProfile: (User) -> Unit
) : RecyclerView.Adapter<SearchFriendAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imgAvatar: ImageView = view.findViewById(R.id.imgAvatar)
        private val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        private val tvEmail: TextView = view.findViewById(R.id.tvEmail)
        private val btnAddFriend: TextView = view.findViewById(R.id.btnAddFriend)

        fun bind(user: User) {
            tvUsername.text = user.username.ifBlank { "Không có tên" }
            tvEmail.text = user.email

            // 🖼 load avatar từ Cloudinary (nếu có)
            if (user.avatarUrl.isNotEmpty()) {
                Glide.with(imgAvatar.context)
                    .load("${user.avatarUrl}?v=${System.currentTimeMillis()}")
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .placeholder(R.drawable.ic_user_placeholder)
                    .error(R.drawable.ic_user_placeholder)
                    .into(imgAvatar)
            } else {
                imgAvatar.setImageResource(R.drawable.ic_user_placeholder)
            }

            // Kết bạn
            btnAddFriend.setOnClickListener {
                onAddFriend(user)
            }

            // Bấm vào cả item -> mở profile
            itemView.setOnClickListener {
                onOpenProfile(user)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_search, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }

    fun setData(newList: List<User>) {
        list = newList.toMutableList()
        notifyDataSetChanged()
    }
}
