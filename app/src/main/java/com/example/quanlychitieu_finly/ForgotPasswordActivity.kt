package com.example.quanlychitieu_finly

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var edtEmail: TextInputEditText
    private lateinit var btnSendOtp: MaterialButton
    private lateinit var tvBackToLogin: TextView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var generatedOTP: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        supportActionBar?.hide()

        edtEmail = findViewById(R.id.edtForgotEmail)
        btnSendOtp = findViewById(R.id.btnSendOtp)
        tvBackToLogin = findViewById(R.id.tvBackToLogin)

        btnSendOtp.setOnClickListener {
            val email = edtEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show()
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show()
            } else {
                checkEmailAndSendOtp(email)
            }
        }

        tvBackToLogin.setOnClickListener {
            finish() // Quay lại màn hình đăng nhập
        }
    }

    // 1. Kiểm tra email có trong hệ thống không
    private fun checkEmailAndSendOtp(email: String) {
        setLoading(true)

        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Email chính xác -> Gửi OTP xác thực
                    sendOtp(email)
                } else {
                    setLoading(false)
                    Toast.makeText(this, "Email này chưa đăng ký tài khoản!", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                setLoading(false)
                Toast.makeText(this, "Lỗi kết nối: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // 2. Gửi mã OTP 6 số
    private fun sendOtp(email: String) {
        generatedOTP = Random.nextInt(100000, 999999).toString()

        val subject = "Mã xác thực Finly"
        val body = "Mã xác thực của bạn là: $generatedOTP\n\nSử dụng mã này để nhận liên kết đặt lại mật khẩu."

        GMailSender.sendEmail(email, subject, body) { isSuccess ->
            runOnUiThread {
                setLoading(false)
                if (isSuccess) {
                    Toast.makeText(this, "Đã gửi mã OTP tới $email", Toast.LENGTH_SHORT).show()
                    showOtpDialog(email) // Hiện bảng nhập OTP
                } else {
                    Toast.makeText(this, "Gửi mail thất bại. Vui lòng thử lại.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 3. Hiện Dialog nhập OTP (Dùng layout dialog_otp_6_digits.xml đã tạo ở bước trước)
    private fun showOtpDialog(email: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_otp_6_digits, null)
        val edtOtpInput = dialogView.findViewById<EditText>(R.id.edtOtpInput)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirmOtp)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelOtp)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Làm nền trong suốt để bo góc đẹp
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

        // Xử lý nút Xác nhận OTP
        btnConfirm.setOnClickListener {
            val inputOtp = edtOtpInput.text.toString().trim()

            if (inputOtp == generatedOTP) {
                dialog.dismiss()
                // OTP ĐÚNG -> Gửi link reset password của Firebase
                sendFirebaseResetLink(email)
            } else {
                edtOtpInput.error = "Mã OTP không đúng"
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
    }

    // 4. Gửi Link Reset Password Chính Chủ
    private fun sendFirebaseResetLink(email: String) {
        // Hiện loading trên màn hình chính để người dùng chờ
        setLoading(true)

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    // Thành công -> Hiện thông báo chốt
                    showSuccessDialog(email)
                } else {
                    Toast.makeText(this, "Lỗi gửi link: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // 5. Thông báo thành công & Về màn hình đăng nhập
    private fun showSuccessDialog(email: String) {
        AlertDialog.Builder(this)
            .setTitle("✅ Đã gửi liên kết")
            .setMessage("Xác thực thành công!\n\nMột email chứa liên kết đặt lại mật khẩu đã được gửi tới: $email\n\nVui lòng kiểm tra hộp thư và bấm vào link để đặt mật khẩu mới.")
            .setPositiveButton("Về đăng nhập") { _, _ ->
                finish() // Đóng trang này, quay về LoginActivity
            }
            .setCancelable(false)
            .show()
    }

    private fun setLoading(isLoading: Boolean) {
        btnSendOtp.isEnabled = !isLoading
        btnSendOtp.text = if (isLoading) "Đang xử lý..." else "Gửi mã xác thực"
    }
}