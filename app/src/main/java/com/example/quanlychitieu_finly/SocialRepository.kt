package com.example.quanlychitieu_finly.social

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration

object SocialRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private fun currentUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Chưa đăng nhập")

    /** Lấy danh sách user khác mình (để kết bạn) */
    fun listenAllUsers(
        onChange: (List<AppUser>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection("users")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onError(e)
                    return@addSnapshotListener
                }
                val myId = auth.currentUser?.uid
                val users = snapshot?.documents
                    ?.mapNotNull { it.toObject(AppUser::class.java)?.copy(id = it.id) }
                    ?.filter { it.id != myId } ?: emptyList()
                onChange(users)
            }
    }

    /** Gửi/ chấp nhận kết bạn đơn giản: tạo doc status=accepted ở cả 2 bên */
    fun addFriend(friendId: String, onDone: (Boolean) -> Unit) {
        val myId = currentUid()
        val now = System.currentTimeMillis()

        val myRef = db.collection("users")
            .document(myId)
            .collection("friends")
            .document(friendId)

        val friendRef = db.collection("users")
            .document(friendId)
            .collection("friends")
            .document(myId)

        val data = hashMapOf(
            "status" to "accepted",
            "createdAt" to now
        )

        db.runBatch { batch ->
            batch.set(myRef, data)
            batch.set(friendRef, data)
        }.addOnSuccessListener { onDone(true) }
            .addOnFailureListener { onDone(false) }
    }

    /** Lắng nghe danh sách bạn bè (status = accepted) */
    fun listenFriends(
        onChange: (List<AppUser>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        val myId = currentUid()
        val friendsRef = db.collection("users")
            .document(myId)
            .collection("friends")
            .whereEqualTo("status", "accepted")

        return friendsRef.addSnapshotListener { snap, e ->
            if (e != null) {
                onError(e)
                return@addSnapshotListener
            }
            val friendIds = snap?.documents?.map { it.id } ?: emptyList()
            if (friendIds.isEmpty()) {
                onChange(emptyList())
                return@addSnapshotListener
            }
            db.collection("users")
                .whereIn("id", friendIds) // nếu bạn có field id trong document
                .get()
                .addOnSuccessListener { qs ->
                    val users = qs.documents.mapNotNull {
                        it.toObject(AppUser::class.java)?.copy(id = it.id)
                    }
                    onChange(users)
                }
                .addOnFailureListener { onError(it) }
        }
    }

    /** Tạo chatId từ 2 uid (đảm bảo cố định) */
    fun createChatId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_$uid2" else "${uid2}_$uid1"
    }

    /** Lắng nghe danh sách cuộc chat của mình */
    fun listenChats(
        onChange: (List<ChatThread>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        val myId = currentUid()
        return db.collection("chats")
            .whereArrayContains("userIds", myId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    onError(e)
                    return@addSnapshotListener
                }
                val chats = snap?.documents?.mapNotNull { doc ->
                    val thread = doc.toObject(ChatThread::class.java)
                    thread?.copy(id = doc.id)
                } ?: emptyList()
                onChange(chats)
            }
    }

    /** Lắng nghe tin nhắn realtime trong 1 chat */
    fun listenMessages(
        chatId: String,
        onChange: (List<ChatMessage>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    onError(e)
                    return@addSnapshotListener
                }
                val messages = snap?.documents?.mapNotNull { doc ->
                    val m = doc.toObject(ChatMessage::class.java)
                    m?.copy(id = doc.id)
                } ?: emptyList()
                onChange(messages)
            }
    }

    /** Gửi tin nhắn realtime */
    fun sendMessage(chatId: String, text: String) {
        val myId = currentUid()
        val msgRef = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .document()

        val now = System.currentTimeMillis()

        val msgData = hashMapOf(
            "senderId" to myId,
            "text" to text,
            "createdAt" to now
        )

        val chatRef = db.collection("chats").document(chatId)
        val chatUpdate = hashMapOf(
            "lastMessage" to text,
            "lastSenderId" to myId,
            "updatedAt" to now
        )

        db.runBatch { batch ->
            batch.set(msgRef, msgData)
            batch.set(chatRef, chatUpdate, com.google.firebase.firestore.SetOptions.merge())
        }
    }

    /** Tạo chat mới & mở chat */
    fun openChatWith(friendId: String, onChatId: (String) -> Unit) {
        val myId = currentUid()
        val chatId = createChatId(myId, friendId)
        val chatRef = db.collection("chats").document(chatId)

        chatRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                val data = hashMapOf(
                    "userIds" to listOf(myId, friendId),
                    "lastMessage" to "",
                    "lastSenderId" to "",
                    "updatedAt" to System.currentTimeMillis()
                )
                chatRef.set(data).addOnSuccessListener { onChatId(chatId) }
            } else {
                onChatId(chatId)
            }
        }
    }
}
