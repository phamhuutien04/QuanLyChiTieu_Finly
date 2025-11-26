package Chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.quanlychitieu_finly.R

class ChatListAdapter(
    private var list: MutableList<ChatListItem>,
    private val onClick: (ChatListItem) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatVH>() {

    fun setData(newList: List<ChatListItem>) {
        list = newList.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_list, parent, false)
        return ChatVH(v)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ChatVH, position: Int) {
        holder.bind(list[position])
    }

    inner class ChatVH(v: View) : RecyclerView.ViewHolder(v) {

        private val avatar: ImageView = v.findViewById(R.id.imgListAvatar)
        private val name: TextView = v.findViewById(R.id.tvListName)
        private val lastMsg: TextView = v.findViewById(R.id.tvListLastMsg)
        private val time: TextView = v.findViewById(R.id.tvListTime)
        private val unreadDot: ImageView = v.findViewById(R.id.imgUnreadDot)

        fun bind(item: ChatListItem) {

            Glide.with(itemView.context).load(item.friendAvatar).into(avatar)

            name.text = item.friendName
            lastMsg.text = item.lastMessage

            time.text = formatTime(item.timestamp)

            // ⭐ HIỆN CHẤM XANH
            unreadDot.visibility = if (item.unread) View.VISIBLE else View.GONE

            itemView.setOnClickListener { onClick(item) }
        }

        private fun formatTime(ts: Long): String {
            if (ts == 0L) return ""
            val sdf = java.text.SimpleDateFormat("HH:mm")
            return sdf.format(java.util.Date(ts))
        }
    }
}
