package com.example.quanlychitieu_finly

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerChat: RecyclerView
    private lateinit var edtMessage: EditText
    private lateinit var btnSend: ImageView
    private lateinit var btnBack: ImageView
    private lateinit var tvChatName: TextView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var adapter: ChatAdapter

    private var chatId = ""
    private var friendUid = ""
    private var currentUid = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        currentUid = auth.currentUser?.uid ?: ""
        chatId = intent.getStringExtra("chatId") ?: ""
        friendUid = intent.getStringExtra("friendUid") ?: ""

        recyclerChat = findViewById(R.id.recyclerChat)
        edtMessage = findViewById(R.id.edtMessage)
        btnSend = findViewById(R.id.btnSend)
        btnBack = findViewById(R.id.btnBack)
        tvChatName = findViewById(R.id.tvChatName)

        loadFriendName()

        adapter = ChatAdapter(currentUid)
        recyclerChat.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        recyclerChat.adapter = adapter

        listenMessages()

        btnSend.setOnClickListener { sendMessage() }
        btnBack.setOnClickListener { finish() }
    }

    private fun loadFriendName() {
        db.collection("users").document(friendUid)
            .get()
            .addOnSuccessListener { doc ->
                tvChatName.text = doc.getString("username") ?: doc.getString("email") ?: "Đang chat"
            }
    }

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

                val messages = snapshot.documents.mapNotNull { doc ->
                    val senderId = doc.getString("senderId") ?: return@mapNotNull null
                    val text = doc.getString("text") ?: ""
                    val ts = doc.getLong("timestamp") ?: 0L
                    ChatMessage(senderId, text, ts)
                }

                adapter.setMessages(messages)
                recyclerChat.scrollToPosition(messages.size - 1)
            }
    }

    private fun sendMessage() {
        val text = edtMessage.text.toString().trim()
        if (text.isEmpty()) return

        val data = hashMapOf(
            "senderId" to currentUid,
            "text" to text,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .add(data)

        edtMessage.setText("")
    }
}
