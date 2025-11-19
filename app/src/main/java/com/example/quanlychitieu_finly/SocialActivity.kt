package com.example.quanlychitieu_finly

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SocialActivity : AppCompatActivity() {

    private lateinit var imgAvatar: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvLocation: TextView
    private lateinit var btnAddFriend: TextView
    private lateinit var btnMessage: TextView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var viewedUid = ""      // UID người được xem
    private var currentUid = ""     // UID người đang dùng

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_social_profile)

        currentUid = auth.currentUser?.uid ?: ""
        viewedUid = intent.getStringExtra("profileUid") ?: ""

        imgAvatar = findViewById(R.id.imgProfileAvatar)
        tvName = findViewById(R.id.tvProfileName)
        tvBio = findViewById(R.id.tvProfileBio)
        tvLocation = findViewById(R.id.tvProfileLocation)
        btnAddFriend = findViewById(R.id.btnAddFriend)
        btnMessage = findViewById(R.id.btnMessage)

        if (viewedUid == currentUid) {
            btnAddFriend.visibility = View.GONE
            btnMessage.visibility = View.GONE
        }

        loadProfileInfo()

        btnAddFriend.setOnClickListener { sendFriendRequest() }
        btnMessage.setOnClickListener { openChat() }
    }

    /** Load thông tin người dùng */
    private fun loadProfileInfo() {
        db.collection("users").document(viewedUid)
            .get()
            .addOnSuccessListener { doc ->
                tvName.text = doc.getString("username") ?: "Không rõ"
                tvBio.text = doc.getString("bio") ?: "Chưa có mô tả"
                tvLocation.text = doc.getString("location") ?: "Không rõ"

                val avatar = doc.getString("avatarUrl") ?: ""
                if (avatar.isNotEmpty()) {
                    Glide.with(this)
                        .load("$avatar?v=${System.currentTimeMillis()}")
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(imgAvatar)
                }
            }
    }

    /** Hàm tạo chatId cố định giữa 2 người */
    private fun generateChatId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    /** Mở trang chat */
    private fun openChat() {
        val chatId = generateChatId(currentUid, viewedUid)

        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("chatId", chatId)
        intent.putExtra("friendUid", viewedUid)
        startActivity(intent)
    }

    /** Gửi lời mời kết bạn */
    private fun sendFriendRequest() {
        val request = hashMapOf(
            "senderId" to currentUid,
            "receiverId" to viewedUid,
            "status" to "pending",
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("friend_requests")
            .add(request)
            .addOnSuccessListener {
                btnAddFriend.text = "Đã gửi ✔"
                btnAddFriend.isEnabled = false
                Toast.makeText(this, "Đã gửi lời mời kết bạn", Toast.LENGTH_SHORT).show()
            }
    }
}
