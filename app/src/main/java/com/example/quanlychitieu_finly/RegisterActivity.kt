package com.example.quanlychitieu_finly

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.cloudinary.Cloudinary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.concurrent.thread
import kotlin.random.Random

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var cloudinary: Cloudinary
    private lateinit var db: FirebaseFirestore

    // Views
    private lateinit var edtUsername: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var edtConfirmPassword: EditText
    private var imgPwdBadge: ImageView? = null
    private lateinit var progressBar: ProgressBar
    private lateinit var btnRegister: Button

    // OTP Variables
    private var generatedOTP: String = ""
    private var otpAttempts = 0 // Biến đếm số lần nhập sai

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        cloudinary = CloudinaryConfig.cloudinaryInstance
        db = FirebaseFirestore.getInstance()

        // Ánh xạ view
        edtUsername = findViewById(R.id.edtUsername)
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword)
        // imgPwdBadge = findViewById(R.id.imgPwdBadge)
        // progressBar = findViewById(R.id.progressBar)

        btnRegister = findViewById(R.id.btnRegister)
        val txtGoLogin = findViewById<TextView>(R.id.txtGoLogin)

        // --- TEXT WATCHER ---
        edtPassword.addTextChangedListener(SimpleTextWatcher { s ->
            val pw = s?.toString() ?: ""
            if (pw.isBlank()) {
                edtPassword.error = null
                imgPwdBadge?.visibility = View.GONE
            } else if (isStrongPassword(pw)) {
                edtPassword.error = null
                imgPwdBadge?.visibility = View.GONE
            } else {
                edtPassword.error = getString(R.string.pwd_error_full)
                imgPwdBadge?.visibility = View.VISIBLE
            }
        })

        edtConfirmPassword.addTextChangedListener(SimpleTextWatcher { s ->
            val pw = edtPassword.text?.toString() ?: ""
            val cpw = s?.toString() ?: ""
            edtConfirmPassword.error = if (cpw.isNotEmpty() && cpw != pw)
                getString(R.string.pwd_not_match) else null
        })

        // --- CLICK ĐĂNG KÝ ---
        btnRegister.setOnClickListener {
            val username = edtUsername.text?.toString()?.trim().orEmpty()
            val email = edtEmail.text?.toString()?.trim().orEmpty()
            val password = edtPassword.text?.toString()?.trim().orEmpty()
            val confirmPassword = edtConfirmPassword.text?.toString()?.trim().orEmpty()

            // Validate
            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edtEmail.error = "Email không hợp lệ"
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                edtConfirmPassword.error = getString(R.string.pwd_not_match)
                return@setOnClickListener
            }
            if (!isStrongPassword(password)) {
                edtPassword.error = getString(R.string.pwd_error_full)
                return@setOnClickListener
            }

            // Bắt đầu quy trình OTP
            sendOtpAndVerify(email)
        }

        txtGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // --- GỬI EMAIL OTP ---
    private fun sendOtpAndVerify(email: String) {
        showLoading(true)

        // Reset lại số lần thử mỗi khi gửi mã mới
        otpAttempts = 0

        // Tạo mã 4 số ngẫu nhiên
        generatedOTP = Random.nextInt(1000, 9999).toString()

        val subject = "Mã xác thực đăng ký Finly"
        val body = "Xin chào,\n\nMã xác thực (OTP) của bạn là: $generatedOTP\n\nMã này có hiệu lực để đăng ký tài khoản mới. Vui lòng không chia sẻ cho người khác."

        GMailSender.sendEmail(email, subject, body) { isSuccess ->
            runOnUiThread {
                showLoading(false)
                if (isSuccess) {
                    Toast.makeText(this, "Đã gửi mã OTP đến $email", Toast.LENGTH_SHORT).show()
                    showOtpDialog(email)
                } else {
                    Toast.makeText(this, "Gửi mail thất bại. Vui lòng kiểm tra lại email!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- HIỆN DIALOG NHẬP OTP ---
    private fun showOtpDialog(email: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_otp_confirm, null)
        val edtOtpInput = dialogView.findViewById<EditText>(R.id.edtOtpInput)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false) // Không cho tắt bằng cách bấm ra ngoài
            .setPositiveButton("Xác nhận", null) // Sẽ override bên dưới
            .setNegativeButton("Hủy") { d, _ ->
                d.dismiss()
                // Có thể reset OTP ở đây nếu muốn hủy hẳn
                generatedOTP = ""
            }
            .create()

        dialog.show()

        // Override nút Positive để không tự động tắt dialog nếu nhập sai
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val inputOtp = edtOtpInput.text.toString().trim()

            if (generatedOTP.isEmpty()) {
                Toast.makeText(this, "Mã đã hết hạn. Vui lòng gửi lại.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                return@setOnClickListener
            }

            if (inputOtp == generatedOTP) {
                // ĐÚNG MÃ -> Cho phép đăng ký
                dialog.dismiss()
                Toast.makeText(this, "Xác thực thành công!", Toast.LENGTH_SHORT).show()
                performRegisterFirebase()
            } else {
                // SAI MÃ -> Xử lý đếm số lần
                otpAttempts++
                val remaining = 3 - otpAttempts

                if (remaining > 0) {
                    edtOtpInput.error = "Mã sai. Còn lại $remaining lần thử."
                    edtOtpInput.setText("") // Xóa ô nhập để nhập lại
                } else {
                    // Hết lượt thử
                    generatedOTP = "" // Hủy hiệu lực mã
                    dialog.dismiss()

                    // Hiện thông báo chặn
                    AlertDialog.Builder(this)
                        .setTitle("Mã hết hiệu lực")
                        .setMessage("Bạn đã nhập sai quá 3 lần. Vui lòng đăng ký lại để lấy mã mới.")
                        .setPositiveButton("Đóng", null)
                        .show()
                }
            }
        }
    }

    // --- ĐĂNG KÝ LÊN FIREBASE (Chỉ chạy khi OTP đúng) ---
    private fun performRegisterFirebase() {
        val username = edtUsername.text.toString().trim()
        val email = edtEmail.text.toString().trim()
        val password = edtPassword.text.toString().trim()

        showLoading(true)

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

                            showLoading(false)
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener {
                            showLoading(false)
                            Toast.makeText(this, "Lỗi lưu dữ liệu: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    showLoading(false)
                    val errorMsg = task.exception?.message ?: "Đăng ký thất bại"
                    if (errorMsg.contains("email address is already in use")) {
                        edtEmail.error = "Email này đã được sử dụng"
                    } else {
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    private fun showLoading(isLoading: Boolean) {
        btnRegister.isEnabled = !isLoading
        btnRegister.text = if (isLoading) "Đang xử lý..." else "Đăng ký"
        // progressBar.isVisible = isLoading
    }

    private fun isStrongPassword(pw: String): Boolean {
        val regex = Regex(
            "^(?=.*[A-Z])(?=.*\\d)(?=.*[!@#\\\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`~]).{6,}\$"
        )
        return regex.containsMatchIn(pw)
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
                        try {
                            val drawable = ContextCompat.getDrawable(this, iconRes) as? BitmapDrawable
                            if (drawable != null) {
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
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
                batch.commit()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun uploadDefaultAvatar(userId: String) {
        thread {
            try {
                val drawable = ContextCompat.getDrawable(this, R.drawable.ic_avt) as? BitmapDrawable
                if (drawable != null) {
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
                    db.collection("users").document(userId).update("avatarUrl", avatarUrl)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}

class SimpleTextWatcher(private val onChanged: (s: CharSequence?) -> Unit) : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { onChanged(s) }
    override fun afterTextChanged(s: Editable?) {}
}