package Chat

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlychitieu_finly.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatListActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ChatListAdapter

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var currentUid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

        val user = auth.currentUser ?: return
        currentUid = user.uid

        recycler = findViewById(R.id.recyclerChatList)
        adapter = ChatListAdapter(mutableListOf()) { item -> openChat(item) }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        loadChatList()
    }

    private fun loadChatList() {

        db.collection("chats")
            .whereArrayContains("members", currentUid)
            .addSnapshotListener { snap, _ ->

                if (snap == null || snap.isEmpty) {
                    adapter.setData(emptyList())
                    return@addSnapshotListener
                }

                val result = mutableListOf<ChatListItem>()
                val total = snap.size()
                var loaded = 0

                for (doc in snap.documents) {

                    val chatId = doc.id
                    val members = doc.get("members") as? List<String> ?: continue
                    val friendUid = members.first { it != currentUid }

                    // Lấy info user
                    db.collection("users").document(friendUid)
                        .get()
                        .addOnSuccessListener { userDoc ->

                            val name = userDoc.getString("username") ?: "Không tên"
                            val avatar = userDoc.getString("avatarUrl") ?: ""

                            // Lấy tin nhắn cuối
                            db.collection("chats")
                                .document(chatId)
                                .collection("messages")
                                .orderBy("timestamp", Query.Direction.DESCENDING)
                                .limit(1)
                                .get()
                                .addOnSuccessListener { msgSnap ->

                                    var lastMsg = "Chưa có tin nhắn"
                                    var ts = 0L

                                    if (!msgSnap.isEmpty) {

                                        val d = msgSnap.documents[0]

                                        val type = d.getString("type") ?: "text"
                                        val text = d.getString("text") ?: ""
                                        val paid = d.getBoolean("paid") ?: false

                                        ts = d.getLong("timestamp") ?: 0L

                                        lastMsg = when (type) {

                                            "text" -> text

                                            "image" -> "📷 Ảnh"

                                            "location_map" -> "📍 Vị trí được chia sẻ"

                                            "request_money" ->
                                                if (paid) "Đã thanh toán"
                                                else "💰 Yêu cầu thanh toán"

                                            else -> "Tin nhắn mới"
                                        }
                                    }

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

                                    loaded++
                                    if (loaded == total) {
                                        result.sortByDescending { it.timestamp }
                                        adapter.setData(result)
                                    }
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
