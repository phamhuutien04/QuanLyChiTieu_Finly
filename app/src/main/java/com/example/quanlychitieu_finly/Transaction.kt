package com.example.quanlychitieu_finly

import com.google.firebase.Timestamp

data class Transaction(
    val title: String = "",
    val categoryId: String = "",     // id document của category
    val categoryName: String = "",
    val categoryIconUrl: String = "",
    val amount: Double = 0.0,
    val type: String = "",           // "income" | "expense"
    val date: Timestamp = Timestamp.now()
)

