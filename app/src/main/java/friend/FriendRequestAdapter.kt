package friend

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.quanlychitieu_finly.R
import com.example.quanlychitieu_finly.User

class FriendRequestAdapter(
    private val list: MutableList<User>,
    private val onAccept: (User) -> Unit,
    private val onReject: (User) -> Unit
) : RecyclerView.Adapter<FriendRequestAdapter.ViewHolder>() {

    inner class ViewHolder(item: View) : RecyclerView.ViewHolder(item) {
        val img: ImageView = item.findViewById(R.id.imgAvatarReq)
        val name: TextView = item.findViewById(R.id.tvNameReq)
        val accept: TextView = item.findViewById(R.id.btnAccept)
        val reject: TextView = item.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_request, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = list[position]

        holder.name.text = user.username

        Glide.with(holder.img.context)
            .load(user.avatarUrl)
            .placeholder(R.drawable.ic_user)
            .circleCrop()
            .into(holder.img)

        holder.accept.setOnClickListener { onAccept(user) }
        holder.reject.setOnClickListener { onReject(user) }
    }

    override fun getItemCount() = list.size

    fun update(newList: List<User>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}
