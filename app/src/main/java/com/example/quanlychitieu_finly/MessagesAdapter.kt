package com.example.quanlychitieu_finly.social

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlychitieu_finly.R

class MessagesAdapter(
    private val currentUserId: String
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DiffCallback) {

    companion object {
        private const val TYPE_SENT = 1
        private const val TYPE_RECEIVED = 2

        object DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage) =
                oldItem == newItem
        }
    }

    override fun getItemViewType(position: Int): Int {
        val msg = getItem(position)
        return if (msg.senderId == currentUserId) TYPE_SENT else TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_SENT) {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_sent, parent, false)
            SentVH(v)
        } else {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_received, parent, false)
            ReceivedVH(v)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = getItem(position)
        when (holder) {
            is SentVH -> holder.bind(msg)
            is ReceivedVH -> holder.bind(msg)
        }
    }

    class SentVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txt: TextView = itemView.findViewById(R.id.txtMessage)
        fun bind(m: ChatMessage) {
            txt.text = m.text
        }
    }

    class ReceivedVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txt: TextView = itemView.findViewById(R.id.txtMessage)
        fun bind(m: ChatMessage) {
            txt.text = m.text
        }
    }
}
