package com.example.quanlychitieu_finly

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SearchFriendsActivity : AppCompatActivity() {

    private lateinit var edtSearch: EditText
    private lateinit var recyclerView: RecyclerView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val allUsers = mutableListOf<User>()
    private lateinit var adapter: SearchFriendAdapter
    private var currentUid = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_friends)

        currentUid = auth.currentUser?.uid ?: ""

        // Ánh xạ view
        edtSearch = findViewById(R.id.edtSearchFriend)
        recyclerView = findViewById(R.id.rcvSearchFriend)

        // Set adapter
        adapter = SearchFriendAdapter(
            mutableListOf(),
            onAddFriend = { user ->
                sendFriendRequest(user)
            },
            onOpenProfile = { user ->
                openProfile(user.id)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadAllUsers()
        setupSearchListener()
    }

    private fun loadAllUsers() {
        db.collection("users").get().addOnSuccessListener { snap ->
            allUsers.clear()

            for (doc in snap.documents) {
                if (doc.id == currentUid) continue

                allUsers.add(
                    User(
                        id = doc.id,
                        username = doc.getString("username") ?: "",
                        email = doc.getString("email") ?: "",
                        avatarUrl = doc.getString("avatarUrl") ?: ""
                    )
                )
            }

            adapter.setData(allUsers)
        }
    }

    private fun setupSearchListener() {
        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val keyword = s.toString().lowercase().trim()

                val filtered = allUsers.filter {
                    it.username.lowercase().contains(keyword)
                }

                adapter.setData(filtered)
            }
        })
    }

    private fun sendFriendRequest(user: User) {
        val request = hashMapOf(
            "senderId" to currentUid,
            "receiverId" to user.id,
            "status" to "pending",
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("friend_requests")
            .add(request)
            .addOnSuccessListener {
                Toast.makeText(this, "Đã gửi lời mời kết bạn!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openProfile(uid: String) {
        val intent = Intent(this, SocialActivity::class.java)
        intent.putExtra("profileUid", uid)
        startActivity(intent)
    }
}
