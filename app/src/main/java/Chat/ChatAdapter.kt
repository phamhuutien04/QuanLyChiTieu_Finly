package Chat

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.quanlychitieu_finly.R
import com.google.firebase.firestore.FirebaseFirestore

class ChatAdapter(
    private val currentUid: String,
    private var friendAvatar: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var messages = listOf<ChatMessage>()

    fun setMessages(list: List<ChatMessage>) {
        messages = list
        notifyDataSetChanged()
    }

    fun setFriendAvatar(url: String) {
        friendAvatar = url
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        val m = messages[position]

        return when (m.type) {
            "image" -> 3
            "location_map" -> 4
            "request_money" ->
                if (m.senderId == currentUid) 6 else 5  // tách ME vs FRIEND
            else -> if (m.senderId == currentUid) 0 else 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return when (viewType) {
            // TEXT ME
            0 -> TextMeVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_text_me, parent, false)
            )

            // TEXT FRIEND
            1 -> TextFriendVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_text_friend, parent, false)
            )

            // IMAGE
            3 -> ImageVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_image, parent, false)
            )

            // LOCATION MAP
            4 -> LocationVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_location, parent, false)
            )

            // REQUEST MONEY - FRIEND (bên trái – có nút thanh toán)
            5 -> RequestMoneyFriendVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_request_money_friend, parent, false)
            )

            // REQUEST MONEY - ME (bên phải – không nút thanh toán)
            6 -> RequestMoneyMeVH(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_request_money_me, parent, false)
            )

            else -> throw IllegalStateException("Unknown viewType")
        }
    }

    override fun getItemCount() = messages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
        val m = messages[pos]

        when (holder) {

            is TextMeVH -> holder.tv.text = m.text

            is TextFriendVH -> {
                holder.tv.text = m.text
                Glide.with(holder.itemView.context)
                    .load(friendAvatar)
                    .circleCrop()
                    .into(holder.avatar)
            }

            is ImageVH -> {
                Glide.with(holder.itemView.context)
                    .load(m.imageUrl)
                    .into(holder.img)

                holder.img.setOnClickListener {
                    val i = Intent(Intent.ACTION_VIEW)
                    i.data = Uri.parse(m.imageUrl)
                    holder.itemView.context.startActivity(i)
                }
            }

            is LocationVH -> {
                Glide.with(holder.itemView.context)
                    .load(m.mapUrl)
                    .into(holder.map)

                holder.map.setOnClickListener {
                    val uri = Uri.parse("geo:${m.latitude},${m.longitude}?q=${m.latitude},${m.longitude}")
                    val i = Intent(Intent.ACTION_VIEW, uri)
                    i.setPackage("com.google.android.apps.maps")
                    holder.itemView.context.startActivity(i)
                }
            }

            is RequestMoneyFriendVH -> holder.bind(m)
            is RequestMoneyMeVH -> holder.bind(m)
        }
    }

    // ======================= VIEW HOLDER ======================= //

    class TextMeVH(v: View) : RecyclerView.ViewHolder(v) {
        val tv: TextView = v.findViewById(R.id.tvTextMe)
    }

    class TextFriendVH(v: View) : RecyclerView.ViewHolder(v) {
        val tv: TextView = v.findViewById(R.id.tvTextFriend)
        val avatar: ImageView = v.findViewById(R.id.imgAvatarFriend)
    }

    class ImageVH(v: View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.imgChatImage)
    }

    class LocationVH(v: View) : RecyclerView.ViewHolder(v) {
        val map: ImageView = v.findViewById(R.id.imgLocationMap)
    }

    // ======= REQUEST MONEY – FRIEND (có nút thanh toán) ======= //

    class RequestMoneyFriendVH(v: View) : RecyclerView.ViewHolder(v) {
        private val amount: TextView = v.findViewById(R.id.tvMoneyValueFriend)
        private val note: TextView = v.findViewById(R.id.tvNoteFriend)
        private val btnPay: TextView = v.findViewById(R.id.btnPayNow)

        fun bind(m: ChatMessage) {
            amount.text = "${m.amount} đ"
            note.text = m.note ?: ""

            if (m.paid) {
                btnPay.text = "Đã thanh toán ✔"
                btnPay.isEnabled = false
            }

            btnPay.setOnClickListener {
                val ref = FirebaseFirestore.getInstance()
                    .collection("chats")
                    .document(m.chatId)
                    .collection("messages")
                    .document(m.msgId)

                ref.update("paid", true)

                btnPay.text = "Đã thanh toán ✔"
                btnPay.isEnabled = false
            }
        }
    }

    // ======= REQUEST MONEY – ME (không nút thanh toán) ======= //

    class RequestMoneyMeVH(v: View) : RecyclerView.ViewHolder(v) {
        private val amount: TextView = v.findViewById(R.id.tvMoneyValueMe)
        private val note: TextView = v.findViewById(R.id.tvNoteMe)
        private val status: TextView = v.findViewById(R.id.tvPaidStatusMe)

        fun bind(m: ChatMessage) {
            amount.text = "${m.amount} đ"
            note.text = m.note ?: ""

            status.text = if (m.paid) "Đã thanh toán ✔" else "Chưa thanh toán"
        }
    }
}
