package com.example.quanlychitieu_finly

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    // FirebaseAuth instance
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        // Khởi tạo FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Ánh xạ view
        val edtUsername = findViewById<EditText>(R.id.edtUsername)
        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val edtConfirmPassword = findViewById<EditText>(R.id.edtConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val txtGoLogin = findViewById<TextView>(R.id.txtGoLogin)

        // Bấm nút đăng ký
        btnRegister.setOnClickListener {
            val username = edtUsername.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()
            val confirmPassword = edtConfirmPassword.text.toString().trim()

            // Kiểm tra dữ liệu nhập
            if (username.isEmpty()) {
                edtUsername.error = "Vui lòng nhập tên người dùng"
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                edtEmail.error = "Vui lòng nhập email"
                return@setOnClickListener
            }
            if (password.isEmpty() || password.length < 6) {
                edtPassword.error = "Mật khẩu phải >= 6 ký tự"
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                edtConfirmPassword.error = "Mật khẩu nhập lại không khớp"
                return@setOnClickListener
            }

            // Đăng ký Firebase
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()

                        // Chuyển sang màn hình Login
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Lỗi: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // Chuyển qua màn hình Login nếu đã có tài khoản
        txtGoLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
