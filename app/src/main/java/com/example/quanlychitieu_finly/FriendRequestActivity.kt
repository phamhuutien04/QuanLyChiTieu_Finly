package com.example.quanlychitieu_finly

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FriendRequestActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var btnBack: ImageView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val list = mutableListOf<User>()
    private lateinit var adapter: FriendRequestAdapter

    private var currentUid = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_request)

        currentUid = auth.currentUser?.uid ?: ""

        recycler = findViewById(R.id.recyclerRequests)
        btnBack = findViewById(R.id.btnBack)

        adapter = FriendRequestAdapter(
            list,
            onAccept = { acceptFriend(it) },
            onReject = { rejectFriend(it) }
        )

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        btnBack.setOnClickListener { finish() }

        loadRequests()
    }

    private fun loadRequests() {
        db.collection("users")
            .document(currentUid)
            .collection("friend_requests")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                val temp = mutableListOf<User>()

                snap.documents.forEach { doc ->
                    val senderId = doc.getString("senderId") ?: return@forEach

                    db.collection("users").document(senderId)
                        .get()
                        .addOnSuccessListener { u ->
                            temp.add(
                                User(
                                    id = senderId,
                                    username = u.getString("username") ?: "",
                                    email = u.getString("email") ?: "",
                                    avatarUrl = u.getString("avatarUrl") ?: ""
                                )
                            )
                            adapter.update(temp)
                        }
                }
            }
    }

    // Accept friend
    private fun acceptFriend(user: User) {

        val since = mapOf("since" to System.currentTimeMillis())

        // both sides
        db.collection("friends").document(currentUid)
            .collection("list").document(user.id).set(since)

        db.collection("friends").document(user.id)
            .collection("list").document(currentUid).set(since)

        // delete request
        deleteRequest(user.id)

        Toast.makeText(this, "Đã là bạn bè!", Toast.LENGTH_SHORT).show()
    }

    // Reject request
    private fun rejectFriend(user: User) {
        deleteRequest(user.id)
        Toast.makeText(this, "Đã xóa lời mời!", Toast.LENGTH_SHORT).show()
    }

    private fun deleteRequest(senderId: String) {
        db.collection("users")
            .document(currentUid)
            .collection("friend_requests")
            .whereEqualTo("senderId", senderId)
            .get()
            .addOnSuccessListener { snap ->
                snap.documents.forEach { it.reference.delete() }
            }
    }
}
