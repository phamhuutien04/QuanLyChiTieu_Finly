package com.example.quanlychitieu_finly

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils

object CloudinaryConfig {
    // 🔹 Thay các giá trị bên dưới bằng thông tin tài khoản Cloudinary của bạn
    const val CLOUD_NAME = "dixilpzlq"
    const val API_KEY = "964262414655476"
    const val API_SECRET = "P7pNYdCzdXNkQ4JU2D-4pDkiNNs"
    const val UPLOAD_PRESET = "upload"

    // 🔹 Tạo 1 instance Cloudinary dùng lại toàn app
    val cloudinaryInstance: Cloudinary by lazy {
        Cloudinary(
            ObjectUtils.asMap(
                "cloud_name", CLOUD_NAME,
                "api_key", API_KEY,
                "api_secret", API_SECRET,
                "secure", true
            )
        )
    }
}
