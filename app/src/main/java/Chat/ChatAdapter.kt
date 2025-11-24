package Chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.quanlychitieu_finly.R

class ChatAdapter(
    private val currentUid: String,
    private var friendAvatar: String

) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages = mutableListOf<ChatMessage>()

    companion object {
        private const val TYPE_ME = 1
        private const val TYPE_THEM = 2
    }

    fun setMessages(list: List<ChatMessage>) {
        messages.clear()
        messages.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUid) TYPE_ME else TYPE_THEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return if (viewType == TYPE_ME) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_me, parent, false)
            MeHolder(view)

        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_them, parent, false)
            ThemHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]

        if (holder is MeHolder) holder.bind(msg)
        if (holder is ThemHolder) holder.bind(msg, friendAvatar)
    }

    override fun getItemCount() = messages.size


    // ----- VIEW HOLDER CỦA MÌNH -----
    class MeHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvText: TextView = itemView.findViewById(R.id.tvMyMessage)

        fun bind(msg: ChatMessage) {
            tvText.text = msg.text
        }
    }
    fun setFriendAvatar(url: String) {
        friendAvatar = url
        notifyDataSetChanged()
    }


    // ----- VIEW HOLDER CỦA NGƯỜI KHÁC -----
    class ThemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvText: TextView = itemView.findViewById(R.id.tvTheirMessage)
        private val imgAvatar: ImageView = itemView.findViewById(R.id.imgTheirAvatar)

        fun bind(msg: ChatMessage, avatarUrl: String) {
            tvText.text = msg.text

            Glide.with(imgAvatar.context)
                .load(avatarUrl)
                .circleCrop()
                .into(imgAvatar)
        }
    }
}
