//
//package com.example.quanlychitieu_finly
//
//import android.content.Intent
//import android.graphics.Bitmap
//import android.graphics.drawable.BitmapDrawable
//import android.os.Bundle
//import android.widget.Button
//import android.widget.EditText
//import android.widget.TextView
//import android.widget.Toast
//import androidx.activity.enableEdgeToEdge
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.content.ContextCompat
//import com.cloudinary.Cloudinary
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//import java.io.ByteArrayInputStream
//import java.io.ByteArrayOutputStream
//import kotlin.concurrent.thread
//
//class RegisterActivity : AppCompatActivity() {
//
//    private lateinit var auth: FirebaseAuth
//    private lateinit var cloudinary: Cloudinary
//    private lateinit var db: FirebaseFirestore
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContentView(R.layout.activity_register)
//
//        // Khởi tạo các instance
//        auth = FirebaseAuth.getInstance()
//        cloudinary = CloudinaryConfig.cloudinaryInstance // Đảm bảo bạn có file CloudinaryConfig
//        db = FirebaseFirestore.getInstance()
//
//        // Ánh xạ View
//        val edtUsername = findViewById<EditText>(R.id.edtUsername)
//        val edtEmail = findViewById<EditText>(R.id.edtEmail)
//        val edtPassword = findViewById<EditText>(R.id.edtPassword)
//        val edtConfirmPassword = findViewById<EditText>(R.id.edtConfirmPassword)
//        val btnRegister = findViewById<Button>(R.id.btnRegister)
//        val txtGoLogin = findViewById<TextView>(R.id.txtGoLogin)
//
//        btnRegister.setOnClickListener {
//            val username = edtUsername.text.toString().trim()
//            val email = edtEmail.text.toString().trim()
//            val password = edtPassword.text.toString().trim()
//            val confirmPassword = edtConfirmPassword.text.toString().trim()
//
//            // Kiểm tra dữ liệu nhập
//            if (username.isEmpty()) {
//                edtUsername.error = "Vui lòng nhập tên người dùng"
//                return@setOnClickListener
//            }
//            if (email.isEmpty()) {
//                edtEmail.error = "Vui lòng nhập email"
//                return@setOnClickListener
//            }
//            if (password.isEmpty() || password.length < 6) {
//                edtPassword.error = "Mật khẩu phải ≥ 6 ký tự"
//                return@setOnClickListener
//            }
//            if (password != confirmPassword) {
//                edtConfirmPassword.error = "Mật khẩu nhập lại không khớp"
//                return@setOnClickListener
//            }
//
//            // Tạo tài khoản Firebase Auth
//            auth.createUserWithEmailAndPassword(email, password)
//                .addOnCompleteListener { task ->
//                    if (task.isSuccessful) {
//                        val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
//
//                        // Dữ liệu người dùng
//                        val user = hashMapOf(
//                            "id" to userId,
//                            "username" to username,
//                            "email" to email,
//                            "balance" to 0L, // 💰 Tiền mặc định = 0
//                            "createdAt" to System.currentTimeMillis()
//                        )
//
//                        // Lưu thông tin user vào Firestore
//                        db.collection("users").document(userId)
//                            .set(user)
//                            .addOnSuccessListener {
//                                Toast.makeText(this, "✅ Đăng ký thành công!", Toast.LENGTH_SHORT).show()
//
//                                // 🚀 GỌI HÀM NÂNG CẤP TẠI ĐÂY
//                                createDefaultCategories(userId)
//
//                                // Chuyển qua LoginActivity
//                                val intent = Intent(this, LoginActivity::class.java)
//                                startActivity(intent)
//                                finish()
//                            }
//                            .addOnFailureListener { e ->
//                                Toast.makeText(this, "❌ Lỗi lưu Firestore: ${e.message}", Toast.LENGTH_LONG).show()
//                            }
//                    } else {
//                        Toast.makeText(this, "❌ Lỗi đăng ký: ${task.exception?.message}", Toast.LENGTH_LONG).show()
//                    }
//                }
//        }
//
//        txtGoLogin.setOnClickListener {
//            startActivity(Intent(this, LoginActivity::class.java))
//            finish()
//        }
//    }
//
//    /**
//     * 🔹 Nâng cấp: Tải ảnh lên Cloudinary VÀ lưu thông tin vào Firestore
//     */
//    private fun createDefaultCategories(userId: String) {
//        // Cấu trúc: Map<Loại, List<Pair<IconRes, Tên>>>
//        val defaultCategories = mapOf(
//            "spending" to listOf(
//                Pair(R.drawable.ic_category_food, "Ăn uống"),
//                Pair(R.drawable.ic_car, "Di chuyển"),
//                Pair(R.drawable.ic_category_shop, "Mua sắm"),
//                Pair(R.drawable.ic_category_billic, "Hóa đơn"),
//                Pair(R.drawable.ic_category_sk, "Y tế"),
//                Pair(R.drawable.ic_cinema, "Giải trí"),
//                Pair(R.drawable.ic_sports, "Thể thao"),
//                Pair(R.drawable.ic_adds, "Khác")
//            ),
//            "income" to listOf(
//                Pair(R.drawable.ic_category_wage, "Lương"),
//                Pair(R.drawable.ic_category_wages, "Thưởng"),
//                Pair(R.drawable.ic_adds, "Quà tặng"),
//                Pair(R.drawable.ic_adds, "Khác")
//            )
//        )
//
//        thread {
//            try {
//                // Dùng batch để thực hiện nhiều thao tác ghi cùng lúc, hiệu quả hơn
//                val batch = db.batch()
//
//                for ((type, categories) in defaultCategories) {
//                    for ((iconRes, name) in categories) {
//                        // 1. Tải ảnh lên Cloudinary
//                        val drawable = ContextCompat.getDrawable(this, iconRes)!!
//                        val bitmap = (drawable as BitmapDrawable).bitmap
//                        val baos = ByteArrayOutputStream()
//                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
//                        val inputStream = ByteArrayInputStream(baos.toByteArray())
//
//                        val publicId = "users/$userId/$type/$name/icon"
//                        val uploadParams = mapOf(
//                            "public_id" to publicId,
//                            "upload_preset" to CloudinaryConfig.UPLOAD_PRESET
//                        )
//                        cloudinary.uploader().upload(inputStream, uploadParams)
//                        inputStream.close()
//
//                        // 2. Lấy URL ảnh vừa tải lên để lưu vào Firestore
//                        val imageUrl = cloudinary.url().generate(publicId)
//
//                        // 3. Chuẩn bị dữ liệu để lưu vào Firestore
//                        // Tạo một document mới trong sub-collection "categories" của user
//                        val categoryDocRef = db.collection("users").document(userId)
//                            .collection("categories").document() // Tạo ID tự động
//
//                        val categoryData = hashMapOf(
//                            "id" to categoryDocRef.id,
//                            "name" to name,
//                            "type" to type, // "spending" hoặc "income"
//                            "iconUrl" to imageUrl,
//                            "totalAmount" to 0L // Số tiền giao dịch ban đầu cho danh mục này = 0
//                        )
//
//                        // Thêm thao tác "set" vào batch, chưa thực thi ngay
//                        batch.set(categoryDocRef, categoryData)
//                    }
//                }
//
//                // 4. Thực thi tất cả các thao tác ghi trong batch một lần duy nhất
//                batch.commit().addOnSuccessListener {
//                    runOnUiThread {
//                        // Toast này chỉ hiện khi cả upload và lưu Firestore đều xong
//                        Toast.makeText(this, "📂 Tạo danh mục mẫu thành công!", Toast.LENGTH_SHORT).show()
//                    }
//                }
//
//            } catch (e: Exception) {
//                runOnUiThread {
//                    Toast.makeText(this, "⚠️ Lỗi khi tạo danh mục: ${e.message}", Toast.LENGTH_LONG).show()
//                }
//            }
//        }
//    }
//}



package com.example.quanlychitieu_finly

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.cloudinary.Cloudinary
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.concurrent.thread
import com.google.firebase.auth.ktx.userProfileChangeRequest


class RegisterActivity : AppCompatActivity() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Cloudinary
    private lateinit var cloudinary: Cloudinary

    // Views
    private lateinit var edtUsername: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var edtConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var txtGoLogin: TextView

    private fun isPasswordValid(password: String): Boolean {
        val passwordRegex =
            Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{6,}$")
        return passwordRegex.matches(password)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        // Init
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        cloudinary = CloudinaryConfig.cloudinaryInstance // đảm bảo đã cấu hình

        // Bind views
        edtUsername = findViewById(R.id.edtUsername)
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        txtGoLogin = findViewById(R.id.txtGoLogin)

        btnRegister.setOnClickListener {
            val username = edtUsername.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()
            val confirmPassword = edtConfirmPassword.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!isPasswordValid(password)) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("🔒 Mật khẩu chưa hợp lệ")
                    .setMessage(
                        """
                        Vui lòng đảm bảo mật khẩu của bạn có:
                        • Ít nhất 6 ký tự
                        • Một chữ thường
                        • Một chữ in hoa
                        • Một chữ số
                        • Một ký tự đặc biệt (@, #, $, %...)
                        """.trimIndent()
                    )
                    .setPositiveButton("OK") { dialog, _ ->
                        edtPassword.text.clear()
                        edtConfirmPassword.text.clear()
                        edtPassword.requestFocus()
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()
                return@setOnClickListener
            }

            // Tạo tài khoản Firebase
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (!task.isSuccessful) {
                        Toast.makeText(this, "Đăng ký thất bại: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }

                    val user = auth.currentUser
                    if (user == null) {
                        Toast.makeText(this, "Không lấy được thông tin người dùng", Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }

                    // Cập nhật displayName
                    val profileUpdates = userProfileChangeRequest { displayName = username }
                    user.updateProfile(profileUpdates)
                        .addOnSuccessListener {
                            // Lưu thông tin user vào Firestore (kèm balance = 0L)
                            val userDoc = hashMapOf(
                                "uid" to user.uid,
                                "username" to username,
                                "email" to email,
                                "balance" to 0L,
                                "createdAt" to Timestamp.now()
                            )

                            db.collection("users").document(user.uid)
                                .set(userDoc)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "✅ Đăng ký thành công!", Toast.LENGTH_SHORT).show()

                                    // Tạo danh mục mặc định + upload icon lên Cloudinary
                                    createDefaultCategories(user.uid)

                                    // Điều hướng sang Login
                                    startActivity(Intent(this, LoginActivity::class.java))
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "❌ Lỗi lưu Firestore: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Lỗi cập nhật tên: ${e.message}", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                }
        }

        txtGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    /**
     * Tạo danh mục mặc định và upload icon lên Cloudinary, lưu iconUrl vào Firestore
     */
    private fun createDefaultCategories(userId: String) {
        val defaultCategories = mapOf(
            "spending" to listOf(
                Pair(R.drawable.ic_category_food, "Ăn uống"),
                Pair(R.drawable.ic_car, "Di chuyển"),
                Pair(R.drawable.ic_category_shop, "Mua sắm"),
                Pair(R.drawable.ic_category_billic, "Hóa đơn"),
                Pair(R.drawable.ic_category_sk, "Y tế"),
                Pair(R.drawable.ic_cinema, "Giải trí"),
                Pair(R.drawable.ic_sports, "Thể thao"),
                Pair(R.drawable.ic_adds, "Khác")
            ),
            "income" to listOf(
                Pair(R.drawable.ic_category_wage, "Lương"),
                Pair(R.drawable.ic_category_wages, "Thưởng"),
                Pair(R.drawable.ic_adds, "Quà tặng"),
                Pair(R.drawable.ic_adds, "Khác")
            )
        )

        thread {
            try {
                val batch = db.batch()

                for ((type, categories) in defaultCategories) {
                    for ((iconRes, name) in categories) {
                        // 1) Upload icon drawable -> Cloudinary
                        val drawable = ContextCompat.getDrawable(this, iconRes)!!
                        val bitmap = (drawable as BitmapDrawable).bitmap
                        val baos = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                        val inputStream = ByteArrayInputStream(baos.toByteArray())

                        val publicId = "users/$userId/$type/$name/icon"
                        val uploadParams = mapOf(
                            "public_id" to publicId,
                            "upload_preset" to CloudinaryConfig.UPLOAD_PRESET
                        )
                        cloudinary.uploader().upload(inputStream, uploadParams)
                        inputStream.close()

                        // 2) Lấy URL ảnh
                        val imageUrl = cloudinary.url().generate(publicId)

                        // 3) Chuẩn bị ghi Firestore
                        val catRef = db.collection("users")
                            .document(userId)
                            .collection("categories")
                            .document() // auto id

                        val catData = hashMapOf(
                            "id" to catRef.id,
                            "name" to name,
                            "type" to type,          // spending | income
                            "iconUrl" to imageUrl,   // từ Cloudinary
                            "totalAmount" to 0L
                        )

                        batch.set(catRef, catData)
                    }
                }

                // 4) Commit batch
                batch.commit().addOnSuccessListener {
                    runOnUiThread {
                        Toast.makeText(this, "📂 Tạo danh mục mẫu thành công!", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { e ->
                    runOnUiThread {
                        Toast.makeText(this, "⚠️ Lỗi commit batch: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "⚠️ Lỗi khi tạo danh mục: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
