package com.example.quanlychitieu_finly

data class Category(
    var id: String = "",
    var name: String = "",
    var iconUrl: String = "",    // URL http(s) hoặc tên resource: "ic_food"
    var type: String = "",       // "spending" | "income"
    var totalAmount: Long = 0L
)
