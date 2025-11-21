package com.example.quanlychitieu_finly

import android.os.Bundle
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
import com.google.firebase.firestore.QuerySnapshot

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerChat: RecyclerView
    private lateinit var edtMessage: EditText
    private lateinit var btnSend: ImageView
    private lateinit var imgChatAvatar: ImageView
    private lateinit var tvChatName: TextView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var chatId = ""
    private var friendUid = ""
    private var currentUid = ""

    private lateinit var adapter: ChatAdapter
    private var friendAvatar = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatId = intent.getStringExtra("chatId") ?: ""
        friendUid = intent.getStringExtra("friendUid") ?: ""
        currentUid = auth.currentUser?.uid ?: ""

        ensureChatDocumentExists()

        recyclerChat = findViewById(R.id.recyclerChat)
        edtMessage = findViewById(R.id.edtMessage)
        btnSend = findViewById(R.id.btnSend)
        imgChatAvatar = findViewById(R.id.imgChatAvatar)
        tvChatName = findViewById(R.id.tvChatName)

        adapter = ChatAdapter(currentUid, "")
        recyclerChat.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        recyclerChat.adapter = adapter

        loadFriendInfo()
        listenMessages()

        btnSend.setOnClickListener { sendMessage() }
    }

    // Tạo document chat + members nếu chưa có
    private fun ensureChatDocumentExists() {
        val chatRef = db.collection("chats").document(chatId)

        chatRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                val data = mapOf(
                    "members" to listOf(currentUid, friendUid)
                )
                chatRef.set(data)
            } else {
                if (!doc.contains("members")) {
                    chatRef.update("members", listOf(currentUid, friendUid))
                }
            }
        }
    }

    private fun loadFriendInfo() {
        db.collection("users").document(friendUid)
            .get()
            .addOnSuccessListener { doc ->

                val name = doc.getString("username") ?: "Đang chat"
                tvChatName.text = name

                friendAvatar = doc.getString("avatarUrl") ?: ""
                Glide.with(this).load(friendAvatar).circleCrop().into(imgChatAvatar)

                adapter.setFriendAvatar(friendAvatar)
            }
    }

    private fun listenMessages() {
        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->

                if (snap == null) return@addSnapshotListener

                val msgs = snap.documents.map { d ->
                    ChatMessage(
                        senderId = d.getString("senderId") ?: "",
                        text = d.getString("text") ?: "",
                        timestamp = d.getLong("timestamp") ?: 0
                    )
                }

                adapter.setMessages(msgs)
                recyclerChat.scrollToPosition(msgs.size - 1)

                // 🔥 Đánh dấu đã đọc
                markMessagesAsSeen(snap)
            }
    }

    private fun markMessagesAsSeen(snap: QuerySnapshot) {
        for (d in snap.documents) {
            val senderId = d.getString("senderId") ?: ""
            if (senderId == currentUid) continue   // tin mình gửi thì bỏ qua

            val seenBy = d.get("seenBy") as? MutableList<String> ?: mutableListOf()
            if (!seenBy.contains(currentUid)) {
                seenBy.add(currentUid)
                d.reference.update("seenBy", seenBy)
            }
        }
    }

    private fun sendMessage() {
        val text = edtMessage.text.toString().trim()
        if (text.isEmpty()) return

        val msg = mapOf(
            "senderId" to currentUid,
            "text" to text,
            "timestamp" to System.currentTimeMillis(),
            // 🔥 Người gửi luôn coi là đã xem
            "seenBy" to listOf(currentUid)
        )

        db.collection("chats").document(chatId)
            .collection("messages")
            .add(msg)

        edtMessage.setText("")
    }
}
