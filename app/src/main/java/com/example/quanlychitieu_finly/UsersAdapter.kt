package com.example.quanlychitieu_finly.social

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlychitieu_finly.R

class UsersAdapter(
    private val onClick: (AppUser) -> Unit
) : ListAdapter<AppUser, UsersAdapter.UserVH>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<AppUser>() {
        override fun areItemsTheSame(oldItem: AppUser, newItem: AppUser) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: AppUser, newItem: AppUser) = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserVH(v)
    }

    override fun onBindViewHolder(holder: UserVH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtUsername: TextView = itemView.findViewById(R.id.txtUsername)
        private val txtEmail: TextView = itemView.findViewById(R.id.txtEmail)

        fun bind(user: AppUser) {
            txtUsername.text = user.username
            txtEmail.text = user.email
            itemView.setOnClickListener { onClick(user) }
        }
    }
}
