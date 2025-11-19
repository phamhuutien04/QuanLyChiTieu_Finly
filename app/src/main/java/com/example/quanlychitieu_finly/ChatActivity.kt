package com.example.quanlychitieu_finly

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerChat: RecyclerView
    private lateinit var edtMessage: EditText
    private lateinit var btnSend: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var tvChatName: TextView
    private lateinit var imgChatAvatar: ImageView   // ⭐ HEADER AVATAR

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var adapter: ChatAdapter

    private var chatId = ""
    private var friendUid = ""
    private var currentUid = ""

    private var friendAvatar = ""   // ⭐ Lưu URL avatar người chat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Lấy UID
        currentUid = auth.currentUser?.uid ?: ""
        chatId = intent.getStringExtra("chatId") ?: ""
        friendUid = intent.getStringExtra("friendUid") ?: ""

        // Ánh xạ view
        recyclerChat = findViewById(R.id.recyclerChat)
        edtMessage = findViewById(R.id.edtMessage)
        btnSend = findViewById(R.id.btnSend)
        btnBack = findViewById(R.id.btnBack)
        tvChatName = findViewById(R.id.tvChatName)
        imgChatAvatar = findViewById(R.id.imgChatAvatar)   // ⭐ THÊM DÒNG NÀY

        // Chuẩn bị RecyclerView + Adapter
        adapter = ChatAdapter(currentUid, friendAvatar)
        recyclerChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        recyclerChat.adapter = adapter

        loadFriendInfo()
        listenMessages()

        btnSend.setOnClickListener { sendMessage() }
        btnBack.setOnClickListener { finish() }
    }

    // ⭐ Load tên + avatar người chat + gán vào header + gán vào adapter
    private fun loadFriendInfo() {
        db.collection("users").document(friendUid)
            .get()
            .addOnSuccessListener { doc ->

                val name = doc.getString("username") ?: doc.getString("email") ?: "Đang chat"
                tvChatName.text = name

                friendAvatar = doc.getString("avatarUrl") ?: ""

                // ⭐ LOAD AVATAR Ở HEADER
                if (friendAvatar.isNotEmpty()) {
                    Glide.with(this)
                        .load(friendAvatar)
                        .circleCrop()
                        .into(imgChatAvatar)
                }

                // ⭐ CẬP NHẬT AVATAR TIN NHẮN
                adapter.setFriendAvatar(friendAvatar)
            }
    }

    // ⭐ Lắng nghe tin nhắn realtime
    private fun listenMessages() {
        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    Log.e("CHAT", "Listen error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                val list = snapshot.documents.mapNotNull { doc ->
                    ChatMessage(
                        senderId = doc.getString("senderId") ?: return@mapNotNull null,
                        text = doc.getString("text") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                }

                adapter.setMessages(list)
                recyclerChat.scrollToPosition(list.size - 1)
            }
    }

    // ⭐ Gửi tin nhắn
    private fun sendMessage() {
        val text = edtMessage.text.toString().trim()
        if (text.isEmpty()) return

        val msgData = hashMapOf(
            "senderId" to currentUid,
            "text" to text,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .add(msgData)

        edtMessage.setText("")
    }
}
