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
            return
        }

        loadProfile()
        checkFriendState()
    }

    private fun loadProfile() {
        db.collection("users").document(viewedUid).get()
            .addOnSuccessListener { doc ->
                tvName.text = doc.getString("username") ?: "Không rõ"

                val avatar = doc.getString("avatarUrl") ?: ""
                Glide.with(this)
                    .load("$avatar?v=${System.currentTimeMillis()}")
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(imgAvatar)
            }
    }

    private fun checkFriendState() {

        btnMessage.visibility = View.GONE  // ẨN mặc định

        db.collection("users")
            .document(currentUid)
            .collection("friends")
            .document(viewedUid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    showFriendUI()
                } else {
                    checkRequests()
                }
            }
    }

    private fun checkRequests() {

        // 2️⃣ Kiểm tra mình đã gửi lời mời?
        db.collection("friend_requests")
            .whereEqualTo("senderId", currentUid)
            .whereEqualTo("receiverId", viewedUid)
            .get()
            .addOnSuccessListener { sent ->
                if (!sent.isEmpty) {
                    btnAddFriend.text = "Đã gửi"
                    btnAddFriend.isEnabled = false
                    return@addOnSuccessListener
                }

                db.collection("friend_requests")
                    .whereEqualTo("senderId", viewedUid)
                    .whereEqualTo("receiverId", currentUid)
                    .get()
                    .addOnSuccessListener { received ->

                        if (!received.isEmpty) {
                            val reqId = received.documents[0].id

                            btnAddFriend.text = "Chấp nhận"
                            btnAddFriend.isEnabled = true

                            btnAddFriend.setOnClickListener {
                                acceptFriendRequest(reqId)
                            }
                        } else {
                            btnAddFriend.text = "Kết bạn"
                            btnAddFriend.isEnabled = true
                            btnAddFriend.setOnClickListener {
                                sendFriendRequest()
                            }
                        }
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

        db.collection("friend_requests")
            .add(req)
            .addOnSuccessListener {
                Toast.makeText(this, "Đã gửi lời mời", Toast.LENGTH_SHORT).show()
                btnAddFriend.text = "Đã gửi"
                btnAddFriend.isEnabled = false
            }
    }

    private fun acceptFriendRequest(reqId: String) {

        val data = mapOf(
            "since" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(currentUid)
            .collection("friends")
            .document(viewedUid)
            .set(data)

        // viewedUser thêm bạn
        db.collection("users")
            .document(viewedUid)
            .collection("friends")
            .document(currentUid)
            .set(data)

        db.collection("friend_requests")
            .document(reqId)
            .delete()

        Toast.makeText(this, "Đã trở thành bạn bè!", Toast.LENGTH_SHORT).show()

        showFriendUI()
    }

    private fun showFriendUI() {
        btnAddFriend.text = "Bạn bè"
        btnAddFriend.isEnabled = false
        btnMessage.visibility = View.VISIBLE

        btnMessage.setOnClickListener { openChat() }
    }

    private fun openChat() {
        val chatId = generateChatId(currentUid, viewedUid)
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("chatId", chatId)
        intent.putExtra("friendUid", viewedUid)
        startActivity(intent)
    }

    private fun generateChatId(a: String, b: String): String {
        return if (a < b) "${a}_${b}" else "${b}_${a}"
    }
}
