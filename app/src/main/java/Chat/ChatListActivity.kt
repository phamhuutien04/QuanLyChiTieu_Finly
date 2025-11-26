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

        currentUid = auth.currentUser?.uid ?: return

        recycler = findViewById(R.id.recyclerChatList)
        adapter = ChatListAdapter(mutableListOf()) { openChat(it) }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        loadChatList()
    }

    private fun loadChatList() {

        db.collection("chats")
            .whereArrayContains("members", currentUid)
            .addSnapshotListener { chatSnap, _ ->

                if (chatSnap == null || chatSnap.isEmpty) {
                    adapter.setData(emptyList())
                    return@addSnapshotListener
                }

                val finalList = mutableListOf<ChatListItem>()

                chatSnap.documents.forEach { chatDoc ->

                    val chatId = chatDoc.id
                    val members = chatDoc.get("members") as? List<String> ?: return@forEach
                    val friendUid = members.first { it != currentUid }

                    // ⭐ LISTEN USER REALTIME
                    db.collection("users").document(friendUid)
                        .addSnapshotListener { userDoc, _ ->

                            if (userDoc == null) return@addSnapshotListener

                            val name = userDoc.getString("username") ?: "Không tên"
                            val avatar = userDoc.getString("avatarUrl") ?: ""

                            // ⭐ LISTEN LAST MESSAGE REALTIME
                            db.collection("chats")
                                .document(chatId)
                                .collection("messages")
                                .orderBy("timestamp", Query.Direction.DESCENDING)
                                .limit(1)
                                .addSnapshotListener { msgSnap, _ ->

                                    var lastMsg = "Chưa có tin nhắn"
                                    var ts = 0L
                                    var unread = false

                                    if (msgSnap != null && !msgSnap.isEmpty) {

                                        val d = msgSnap.documents[0]

                                        val type = d.getString("type") ?: "text"
                                        val text = d.getString("text") ?: ""
                                        val sender = d.getString("senderId") ?: ""
                                        val paid = d.getBoolean("paid") ?: false
                                        val seenBy = d.get("seenBy") as? List<String> ?: emptyList()

                                        ts = d.getLong("timestamp") ?: 0L

                                        unread = sender != currentUid && !seenBy.contains(currentUid)

                                        lastMsg = when (type) {
                                            "text" -> text
                                            "image" -> "📷 Ảnh"
                                            "location_map" -> "📍 Vị trí"
                                            "request_money" ->
                                                if (paid) "✔ Đã thanh toán"
                                                else "💰 Yêu cầu thanh toán"
                                            else -> "Tin nhắn mới"
                                        }
                                    }

                                    // Xóa mục cũ nếu tồn tại
                                    finalList.removeAll { it.chatId == chatId }

                                    // Thêm mục mới
                                    finalList.add(
                                        ChatListItem(
                                            chatId = chatId,
                                            friendUid = friendUid,
                                            friendName = name,
                                            friendAvatar = avatar,
                                            lastMessage = lastMsg,
                                            timestamp = ts,
                                            unread = unread
                                        )
                                    )

                                    // Sort + update UI
                                    finalList.sortByDescending { it.timestamp }
                                    adapter.setData(finalList)
                                }
                        }
                }
            }
    }

    private fun openChat(item: ChatListItem) {
        val i = Intent(this, ChatActivity::class.java)
        i.putExtra("chatId", item.chatId)
        i.putExtra("friendUid", item.friendUid)
        startActivity(i)
    }
}
