package com.example.quanlychitieu_finly.social

data class FriendStatus(
    val friendId: String = "",
    val status: String = "pending", // accepted, requested, pending
    val createdAt: Long = System.currentTimeMillis()
)
