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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.concurrent.thread
import java.text.Normalizer
import java.util.regex.Pattern

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var cloudinary: Cloudinary
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        cloudinary = CloudinaryConfig.cloudinaryInstance
        db = FirebaseFirestore.getInstance()

        val edtUsername = findViewById<EditText>(R.id.edtUsername)
        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val edtConfirmPassword = findViewById<EditText>(R.id.edtConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val txtGoLogin = findViewById<TextView>(R.id.txtGoLogin)

        btnRegister.setOnClickListener {
            val username = edtUsername.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()
            val confirmPassword = edtConfirmPassword.text.toString().trim()

            // Chỉ giữ lại kiểm tra lỗi cần thiết để tránh lỗi crash
            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) return@setOnClickListener
            if (password.length < 6 || password != confirmPassword) return@setOnClickListener

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                        val user = hashMapOf(
                            "id" to userId,
                            "username" to username,
                            "email" to email,
                            "balance" to 0L,
                            "createdAt" to System.currentTimeMillis()
                        )

                        db.collection("users").document(userId)
                            .set(user)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                                createDefaultCategories(userId)
                                uploadDefaultAvatar(userId)
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }
                    }
                }
        }

        txtGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

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
                Pair(R.drawable.ic_gift, "Quà tặng"),
                Pair(R.drawable.ic_adds, "Khác")
            )
        )

        thread {
            try {
                val batch = db.batch()

                for ((type, categories) in defaultCategories) {
                    for ((iconRes, name) in categories) {
                        val drawable = ContextCompat.getDrawable(this, iconRes)!!
                        val bitmap = (drawable as BitmapDrawable).bitmap
                        val baos = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                        val inputStream = ByteArrayInputStream(baos.toByteArray())

                        val folderPath = "users/$userId/$type/$name"
                        val uploadParams = mapOf(
                            "folder" to folderPath,
                            "public_id" to "icon",
                            "overwrite" to true
                        )

                        cloudinary.uploader().upload(inputStream, uploadParams)
                        inputStream.close()

                        val imageUrl = cloudinary.url().generate("$folderPath/icon")

                        val categoryDocRef = db.collection("users").document(userId)
                            .collection("categories").document()

                        val categoryData = hashMapOf(
                            "id" to categoryDocRef.id,
                            "name" to name,
                            "type" to type,
                            "iconUrl" to imageUrl,
                            "totalAmount" to 0L
                        )

                        batch.set(categoryDocRef, categoryData)
                    }
                }

                batch.commit()

            } catch (_: Exception) {
            }
        }
    }

    private fun uploadDefaultAvatar(userId: String) {
        thread {
            val drawable = ContextCompat.getDrawable(this, R.drawable.ic_avt)!!
            val bitmap = (drawable as BitmapDrawable).bitmap
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val inputStream = ByteArrayInputStream(baos.toByteArray())

            val uploadParams = mapOf(
                "folder" to "users/$userId/avt",
                "public_id" to "avatar",
                "overwrite" to true
            )

            cloudinary.uploader().upload(inputStream, uploadParams)
            inputStream.close()

            val avatarUrl = cloudinary.url().generate("users/$userId/avt/avatar")

            db.collection("users").document(userId)
                        .update("avatarUrl", avatarUrl)

        }
    }
}


