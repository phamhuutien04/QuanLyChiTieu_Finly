package com.example.quanlychitieu_finly.social

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val createdAt: Long = 0L
)
