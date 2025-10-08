package com.example.quanlychitieu_finly


import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var edtForgotEmail: TextInputEditText
    private lateinit var btnResetPassword: MaterialButton
    private lateinit var btnBackToLogin: MaterialButton
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        // Ẩn ActionBar để giao diện đẹp hơn
        supportActionBar?.hide()

        mAuth = FirebaseAuth.getInstance()
        edtForgotEmail = findViewById(R.id.edtForgotEmail)
        btnResetPassword = findViewById(R.id.btnResetPassword)
        btnBackToLogin = findViewById(R.id.btnBackToLogin)

        btnResetPassword.setOnClickListener {
            val email = edtForgotEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show()
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show()
            } else {
                // Disable button để tránh click nhiều lần
                btnResetPassword.isEnabled = false

                mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        btnResetPassword.isEnabled = true

                        if (task.isSuccessful) {
                            AlertDialog.Builder(this)
                                .setTitle("✅ Thành công")
                                .setMessage("Email đặt lại mật khẩu đã được gửi tới $email\n\nVui lòng kiểm tra hộp thư của bạn.")
                                .setPositiveButton("OK") { _, _ -> finish() }
                                .setCancelable(false)
                                .show()
                        } else {
                            AlertDialog.Builder(this)
                                .setTitle("❌ Lỗi")
                                .setMessage("Không thể gửi email. Vui lòng kiểm tra lại địa chỉ email hoặc thử lại sau.")
                                .setPositiveButton("Thử lại") { dialog, _ -> dialog.dismiss() }
                                .show()
                        }
                    }
            }
        }

        // Xử lý nút quay về
        btnBackToLogin.setOnClickListener {
            finish() // Đóng trang ForgotPasswordActivity và quay về LoginActivity
        }
    }
}