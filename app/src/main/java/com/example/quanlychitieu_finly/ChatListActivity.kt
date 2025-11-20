package com.example.quanlychitieu_finly

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChatListActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ChatListAdapter

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var currentUid = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

        currentUid = auth.currentUser?.uid ?: ""

        recycler = findViewById(R.id.recyclerChatList)
        adapter = ChatListAdapter(mutableListOf()) { item ->
            openChat(item)
        }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        loadChatList()
    }

    private fun loadChatList() {
        db.collection("chats")
            .whereArrayContains("members", currentUid)
            .get()
            .addOnSuccessListener { snap ->
                val result = mutableListOf<ChatListItem>()

                for (doc in snap.documents) {
                    val chatId = doc.id
                    val members = doc.get("members") as List<String>

                    val friendUid = members.first { it != currentUid }

                    db.collection("users").document(friendUid)
                        .get()
                        .addOnSuccessListener { userDoc ->

                            val name = userDoc.getString("username") ?: ""
                            val avatar = userDoc.getString("avatarUrl") ?: ""

                            db.collection("chats")
                                .document(chatId)
                                .collection("messages")
                                .orderBy("timestamp")
                                .limitToLast(1)
                                .get()
                                .addOnSuccessListener { msgSnap ->
                                    val lastMsg = if (!msgSnap.isEmpty)
                                        msgSnap.documents[0].getString("text") ?: ""
                                    else
                                        "Chưa có tin nhắn"

                                    val ts = if (!msgSnap.isEmpty)
                                        msgSnap.documents[0].getLong("timestamp") ?: 0L
                                    else 0L

                                    result.add(
                                        ChatListItem(
                                            chatId = chatId,
                                            friendUid = friendUid,
                                            friendName = name,
                                            friendAvatar = avatar,
                                            lastMessage = lastMsg,
                                            timestamp = ts
                                        )
                                    )

                                    adapter.setData(result)
                                }
                        }
                }
            }
    }

    private fun openChat(item: ChatListItem) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("chatId", item.chatId)
        intent.putExtra("friendUid", item.friendUid)
        startActivity(intent)
    }
}
