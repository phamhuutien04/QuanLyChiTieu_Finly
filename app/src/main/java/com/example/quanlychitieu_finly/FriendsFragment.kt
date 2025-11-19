package com.example.quanlychitieu_finly.social

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlychitieu_finly.ChatActivity
import com.example.quanlychitieu_finly.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class FriendsFragment : Fragment() {

    private lateinit var edtSearchUser: EditText
    private lateinit var rvUsers: RecyclerView
    private lateinit var adapter: UsersAdapter

    private var listener: ListenerRegistration? = null
    private var allUsers: List<AppUser> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_friends, container, false)
        edtSearchUser = v.findViewById(R.id.edtSearchUser)
        rvUsers = v.findViewById(R.id.rvUsers)

        adapter = UsersAdapter { user ->
            // khi click 1 user -> tạo chat & mở ChatActivity
            SocialRepository.openChatWith(user.id) { chatId ->
                val intent = Intent(requireContext(), ChatActivity::class.java)
                intent.putExtra("chatId", chatId)
                intent.putExtra("otherName", user.username)
                startActivity(intent)
            }
        }
        rvUsers.adapter = adapter

        edtSearchUser.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterUsers(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        startListenUsers()

        return v
    }

    private fun startListenUsers() {
        listener?.remove()
        listener = SocialRepository.listenAllUsers(
            onChange = { list ->
                allUsers = list
                filterUsers(edtSearchUser.text.toString())
            },
            onError = { /* TODO: show error */ }
        )
    }

    private fun filterUsers(keyword: String) {
        if (keyword.isBlank()) {
            adapter.submitList(allUsers)
        } else {
            val k = keyword.lowercase()
            adapter.submitList(
                allUsers.filter {
                    it.username.lowercase().contains(k) || it.email.lowercase().contains(k)
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove()
    }
}
