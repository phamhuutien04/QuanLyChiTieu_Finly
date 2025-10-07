package com.example.quanlychitieu_finly

data class Category(
    var id: String = "",
    var name: String = "",
    var iconUrl: String = "",   // URL hoặc đường dẫn drawable
    var type: String = "",      // "spending" hoặc "income"
    var totalAmount: Long = 0L
)
