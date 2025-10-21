package com.example.quanlychitieu_finly

data class Category(
    var id: String = "",
    var name: String = "",
    var iconUrl: String = "",    // URL http(s) hoặc tên resource: "ic_food"
    var type: String = "",       // "spending" | "income"
    var colorHex: String = "#B0BEC5", // <-- thêm, mặc định xám nhạt
    var totalAmount: Long = 0L
)
