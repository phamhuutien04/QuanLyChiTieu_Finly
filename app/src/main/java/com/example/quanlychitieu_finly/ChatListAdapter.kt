package com.example.quanlychitieu_finly

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
class ChatListAdapter(
    private val list: MutableList<ChatListItem>,
    private val onClick: (ChatListItem) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatListVH>() {

    inner class ChatListVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAvatar: ImageView = itemView.findViewById(R.id.imgListAvatar)
        val tvName: TextView = itemView.findViewById(R.id.tvListName)
        val tvLastMsg: TextView = itemView.findViewById(R.id.tvListLastMsg)
        val tvTime: TextView = itemView.findViewById(R.id.tvListTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatListVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_list, parent, false)
        return ChatListVH(view)
    }

    override fun onBindViewHolder(holder: ChatListVH, position: Int) {
        val item = list[position]

        holder.tvName.text = item.friendName
        holder.tvLastMsg.text = item.lastMessage

        Glide.with(holder.imgAvatar.context)
            .load(item.friendAvatar)
            .circleCrop()
            .into(holder.imgAvatar)

        if (item.timestamp > 0) {
            holder.tvTime.text =
                android.text.format.DateFormat.format("HH:mm", item.timestamp)
        } else {
            holder.tvTime.text = ""
        }

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = list.size

    fun setData(newList: List<ChatListItem>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}
