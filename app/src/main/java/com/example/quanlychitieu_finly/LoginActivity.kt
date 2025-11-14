//package com.example.quanlychitieu_finly
//
//import android.content.Intent
//import android.os.Bundle
//import android.widget.*
//import androidx.activity.enableEdgeToEdge
//import androidx.appcompat.app.AppCompatActivity
//import com.google.firebase.auth.FirebaseAuth
//
//class LoginActivity : AppCompatActivity() {
//
//    private lateinit var auth: FirebaseAuth
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContentView(R.layout.activity_login)
//
//        // Khởi tạo FirebaseAuth
//        auth = FirebaseAuth.getInstance()
//
//        // Ánh xạ view
//        val edtEmail = findViewById<EditText>(R.id.edtEmail)
//        val edtPassword = findViewById<EditText>(R.id.edtPassword)
//        val btnLogin = findViewById<Button>(R.id.btnLogin)
//        val txtForgotPassword = findViewById<TextView>(R.id.txtForgotPassword)
//        val txtGoRegister = findViewById<TextView>(R.id.txtGoRegister)
//
//        // Xử lý đăng nhập
//        btnLogin.setOnClickListener {
//            val email = edtEmail.text.toString().trim()
//            val password = edtPassword.text.toString().trim()
//
//            if (email.isEmpty()) {
//                edtEmail.error = "Vui lòng nhập email"
//                return@setOnClickListener
//            }
//            if (password.isEmpty()) {
//                edtPassword.error = "Vui lòng nhập mật khẩu"
//                return@setOnClickListener
//            }
//
//            auth.signInWithEmailAndPassword(email, password)
//                .addOnCompleteListener { task ->
//                    if (task.isSuccessful) {
//                        Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
//
//                        // Chuyển sang màn hình chính (MainActivity chẳng hạn)
//                        val intent = Intent(this, MainActivity::class.java)
//                        startActivity(intent)
//                        finish()
//                    } else {
//                        Toast.makeText(this, "Lỗi: ${task.exception?.message}", Toast.LENGTH_LONG).show()
//                    }
//                }
//        }
//
//        // Quên mật khẩu
//        txtForgotPassword.setOnClickListener {
//            val email = edtEmail.text.toString().trim()
//            if (email.isEmpty()) {
//                Toast.makeText(this, "Vui lòng nhập email để đặt lại mật khẩu", Toast.LENGTH_SHORT).show()
//            } else {
//                auth.sendPasswordResetEmail(email)
//                    .addOnCompleteListener { task ->
//                        if (task.isSuccessful) {
//                            Toast.makeText(this, "Đã gửi email đặt lại mật khẩu!", Toast.LENGTH_SHORT).show()
//                        } else {
//                            Toast.makeText(this, "Lỗi: ${task.exception?.message}", Toast.LENGTH_LONG).show()
//                        }
//                    }
//            }
//        }
//
//        // Chuyển sang màn hình đăng ký
//        txtGoRegister.setOnClickListener {
//            val intent = Intent(this, RegisterActivity::class.java)
//            startActivity(intent)
//            finish()
//        }
//    }
//}







package com.example.quanlychitieu_finly

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import android.text.method.PasswordTransformationMethod

class LoginActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var cbShowPassword: CheckBox
    private lateinit var btnLogin: Button
    private lateinit var txtGoRegister: TextView
    private lateinit var txtForgotPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()

        // Ánh xạ view
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        cbShowPassword = findViewById(R.id.cbShowPassword)
        btnLogin = findViewById(R.id.btnLogin)
        txtGoRegister = findViewById(R.id.txtGoRegister)
        txtForgotPassword = findViewById(R.id.txtForgotPassword)

        // Hiện/ẩn mật khẩu
        cbShowPassword.setOnCheckedChangeListener { _, checked ->
            val start = edtPassword.selectionStart
            val end = edtPassword.selectionEnd
            if (checked) {
                edtPassword.transformationMethod = null
            } else {
                edtPassword.transformationMethod =
                    PasswordTransformationMethod.getInstance()
            }
            // Giữ vị trí con trỏ sau khi đổi
            edtPassword.setSelection(start.coerceAtLeast(0), end.coerceAtLeast(0))
        }

        // Nhấn Done trên bàn phím để login
        edtPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin()
                true
            } else false
        }

        // Xử lý login khi bấm nút
        btnLogin.setOnClickListener { attemptLogin() }

        // Chuyển sang trang đăng ký
        txtGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Quên mật khẩu
        txtForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun attemptLogin() {
        val email = edtEmail.text.toString().trim()
        val password = edtPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ email và mật khẩu", Toast.LENGTH_SHORT).show()
            return
        }

        // Khóa nút để tránh bấm nhiều lần
        btnLogin.isEnabled = false

        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                btnLogin.isEnabled = true

                if (task.isSuccessful) {
                    Log.d("Login", "signInWithEmail:success")
                    Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Log.w("Login", "signInWithEmail:failure", task.exception)
                    Toast.makeText(this, "Sai email hoặc mật khẩu!", Toast.LENGTH_SHORT).show()
                }
            }
    }
}

