package com.example.quanlychitieu_finly

data class ChatListItem(
    val chatId: String = "",
    val friendUid: String = "",
    val friendName: String = "",
    val friendAvatar: String = "",
    val lastMessage: String = "",
    val timestamp: Long = 0L
)
