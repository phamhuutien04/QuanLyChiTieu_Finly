package com.example.quanlychitieu_finly

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.cloudinary.Cloudinary
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.concurrent.thread
import android.text.Editable
import android.text.TextWatcher

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var cloudinary: Cloudinary
    private lateinit var db: FirebaseFirestore

    // Views
    private lateinit var tilPassword: TextInputLayout
    private lateinit var edtPassword: TextInputEditText
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var edtConfirmPassword: TextInputEditText
    private var imgPwdBadge: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        cloudinary = CloudinaryConfig.cloudinaryInstance
        db = FirebaseFirestore.getInstance()

        val tilUsername = findViewById<TextInputLayout>(R.id.tilUsername)
        val tilEmail = findViewById<TextInputLayout>(R.id.tilEmail)
        val edtUsername = findViewById<TextInputEditText>(R.id.edtUsername)
        val edtEmail = findViewById<TextInputEditText>(R.id.edtEmail)
        tilPassword = findViewById(R.id.tilPassword)
        edtPassword = findViewById(R.id.edtPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword)

        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val txtGoLogin = findViewById<TextView>(R.id.txtGoLogin)
        // Lắng nghe để hiện/ẩn badge + error realtime
        edtPassword.addTextChangedListener(SimpleTextWatcher {
            val pw = it?.toString() ?: ""
            if (pw.isBlank()) {
                tilPassword.error = null
                imgPwdBadge?.visibility = View.GONE
            } else if (isStrongPassword(pw)) {
                tilPassword.error = null
                imgPwdBadge?.visibility = View.GONE
            } else {
                tilPassword.error = getString(R.string.pwd_error_full)
                imgPwdBadge?.visibility = View.VISIBLE
            }
        })

        edtConfirmPassword.addTextChangedListener(SimpleTextWatcher {
            val pw = edtPassword.text?.toString() ?: ""
            val cpw = it?.toString() ?: ""
            tilConfirmPassword.error = if (cpw.isNotEmpty() && cpw != pw)
                getString(R.string.pwd_not_match) else null
        })

        btnRegister.setOnClickListener {
            val username = edtUsername.text?.toString()?.trim().orEmpty()
            val email = edtEmail.text?.toString()?.trim().orEmpty()
            val password = edtPassword.text?.toString()?.trim().orEmpty()
            val confirmPassword = edtConfirmPassword.text?.toString()?.trim().orEmpty()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                tilConfirmPassword.error = getString(R.string.pwd_not_match)
                return@setOnClickListener
            } else {
                tilConfirmPassword.error = null
            }

            if (!isStrongPassword(password)) {
                tilPassword.error = getString(R.string.pwd_error_full)
                imgPwdBadge?.visibility = View.VISIBLE
                return@setOnClickListener
            } else {
                tilPassword.error = null
                imgPwdBadge?.visibility = View.GONE
            }

            // Tạo tài khoản Firebase
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
                                Toast.makeText(this, getString(R.string.register_success), Toast.LENGTH_SHORT).show()
                                createDefaultCategories(userId)
                                uploadDefaultAvatar(userId)
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Lỗi lưu dữ liệu: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        Toast.makeText(this, "Đăng ký thất bại: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        txtGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    /** Quy tắc: ≥6 ký tự, có ≥1 chữ hoa, ≥1 chữ số, ≥1 ký tự đặc biệt */
    private fun isStrongPassword(pw: String): Boolean {
        val regex = Regex(
            "^(?=.*[A-Z])(?=.*\\d)(?=.*[!@#\\\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`~]).{6,}\$"
        )
        return regex.containsMatchIn(pw)
    }

    /** Tạo categories mặc định và upload icon lên Cloudinary -> lưu vào Firestore */
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
                        val drawable = ContextCompat.getDrawable(this, iconRes) as BitmapDrawable
                        val bitmap = drawable.bitmap
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** Upload avatar mặc định lên Cloudinary và update Firestore */
    private fun uploadDefaultAvatar(userId: String) {
        thread {
            try {
                val drawable = ContextCompat.getDrawable(this, R.drawable.ic_avt) as BitmapDrawable
                val bitmap = drawable.bitmap
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}

class SimpleTextWatcher(private val onChanged: (s: CharSequence?) -> Unit) : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        onChanged(s)
    }
    override fun afterTextChanged(s: Editable?) {}
}
