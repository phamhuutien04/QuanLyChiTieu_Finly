package com.example.quanlychitieu_finly.social

data class ChatThread(
    val id: String = "",
    val userIds: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastSenderId: String = "",
    val updatedAt: Long = 0L
)
