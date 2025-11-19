package com.example.quanlychitieu_finly

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlychitieu_finly.social.ChatMessage
import com.example.quanlychitieu_finly.social.MessagesAdapter
import com.example.quanlychitieu_finly.social.SocialRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class ChatActivity : AppCompatActivity() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var edtMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var adapter: MessagesAdapter

    private var chatId: String = ""
    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbarChat)
        rvMessages = findViewById(R.id.rvMessages)
        edtMessage = findViewById(R.id.edtMessage)
        btnSend = findViewById(R.id.btnSend)

        val otherName = intent.getStringExtra("otherName") ?: "Chat"
        chatId = intent.getStringExtra("chatId") ?: ""

        toolbar.title = otherName
        toolbar.setNavigationOnClickListener { finish() }

        adapter = MessagesAdapter(FirebaseAuth.getInstance().uid ?: "")
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvMessages.adapter = adapter

        startListenMessages()

        btnSend.setOnClickListener {
            val text = edtMessage.text.toString().trim()
            if (text.isNotEmpty() && chatId.isNotEmpty()) {
                SocialRepository.sendMessage(chatId, text)
                edtMessage.setText("")
            }
        }
    }

    private fun startListenMessages() {
        listener?.remove()
        listener = SocialRepository.listenMessages(
            chatId = chatId,
            onChange = { list ->
                adapter.submitList(list)
                if (list.isNotEmpty()) {
                    rvMessages.scrollToPosition(list.size - 1)
                }
            },
            onError = { /* TODO: show error */ }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.remove()
    }
}
