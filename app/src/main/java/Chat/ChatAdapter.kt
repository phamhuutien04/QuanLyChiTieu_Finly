package Chat

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.quanlychitieu_finly.R

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
            else -> if (m.senderId == currentUid) 0 else 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return when (viewType) {

            0 -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_text_me, parent, false)
                TextMeVH(v)
            }

            1 -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_text_friend, parent, false)
                TextFriendVH(v)
            }

            3 -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_image, parent, false)
                ImageVH(v)
            }

            4 -> {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_location, parent, false)
                LocationVH(v)
            }

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
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    intent.setPackage("com.google.android.apps.maps")
                    holder.itemView.context.startActivity(intent)
                }
            }
        }
    }

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
}
