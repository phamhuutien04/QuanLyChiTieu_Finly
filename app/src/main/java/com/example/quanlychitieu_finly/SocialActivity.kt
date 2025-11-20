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
    private lateinit var btnAddFriend: TextView
    private lateinit var btnMessage: TextView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var viewedUid = ""
    private var currentUid = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_social_profile)

        viewedUid = intent.getStringExtra("profileUid") ?: ""
        currentUid = auth.currentUser?.uid ?: ""

        imgAvatar = findViewById(R.id.imgProfileAvatar)
        tvName = findViewById(R.id.tvProfileName)
        btnAddFriend = findViewById(R.id.btnAddFriend)
        btnMessage = findViewById(R.id.btnMessage)

        if (viewedUid == currentUid) {
            btnAddFriend.visibility = View.GONE
            btnMessage.visibility = View.GONE
        }

        loadProfileInfo()
        checkFriendStatus()

        btnAddFriend.setOnClickListener { sendFriendRequest() }
        btnMessage.setOnClickListener { openChat() }
    }

    private fun loadProfileInfo() {
        db.collection("users").document(viewedUid)
            .get()
            .addOnSuccessListener { doc ->

                tvName.text = doc.getString("username") ?: "Không rõ"

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

    private fun checkFriendStatus() {
        db.collection("friends")
            .document(currentUid)
            .collection("list")
            .document(viewedUid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    btnAddFriend.text = "Bạn bè"
                    btnAddFriend.isEnabled = false
                }
            }
    }

    private fun sendFriendRequest() {
        val req = hashMapOf(
            "senderId" to currentUid,
            "receiverId" to viewedUid,
            "status" to "pending",
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(viewedUid)
            .collection("friend_requests")
            .add(req)
            .addOnSuccessListener {
                Toast.makeText(this, "Đã gửi lời mời!", Toast.LENGTH_SHORT).show()
                btnAddFriend.text = "Đã gửi"
                btnAddFriend.isEnabled = false
            }
    }

    private fun openChat() {
        val chatId = generateChatId(currentUid, viewedUid)
        val chatRef = db.collection("chats").document(chatId)

        chatRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                chatRef.set(
                    mapOf("members" to listOf(currentUid, viewedUid))
                )
            }

            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("chatId", chatId)
            intent.putExtra("friendUid", viewedUid)
            startActivity(intent)
        }
    }

    private fun generateChatId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_$uid2" else "${uid2}_$uid1"
    }
}
