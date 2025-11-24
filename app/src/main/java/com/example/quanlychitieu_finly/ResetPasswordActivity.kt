package com.example.quanlychitieu_finly

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import kotlin.text.any
import kotlin.text.isDigit
import kotlin.text.isEmpty
import kotlin.text.isNotEmpty
import kotlin.text.isUpperCase
import kotlin.text.trim
import kotlin.toString

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var edtNewPassword: TextInputEditText
    private lateinit var edtConfirmPassword: TextInputEditText
    private lateinit var btnConfirmReset: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var tvMinLength: TextView
    private lateinit var tvHasNumber: TextView
    private lateinit var tvHasUpperCase: TextView
    private lateinit var mAuth: FirebaseAuth
    private lateinit var oobCode: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        // Ẩn ActionBar
        supportActionBar?.hide()

        mAuth = FirebaseAuth.getInstance()

        // Lấy oobCode từ link email (nếu có)
        oobCode = intent.getStringExtra("oobCode") ?: ""

        edtNewPassword = findViewById(R.id.edtNewPassword)
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword)
        btnConfirmReset = findViewById(R.id.btnConfirmReset)
        btnCancel = findViewById(R.id.btnCancel)
        tvMinLength = findViewById(R.id.tvMinLength)
        tvHasNumber = findViewById(R.id.tvHasNumber)
        tvHasUpperCase = findViewById(R.id.tvHasUpperCase)

        // Validate mật khẩu theo thời gian thực
        edtNewPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validatePasswordStrength(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnConfirmReset.setOnClickListener {
            val newPassword = edtNewPassword.text.toString().trim()
            val confirmPassword = edtConfirmPassword.text.toString().trim()

            when {
                newPassword.isEmpty() -> {
                    Toast.makeText(this, "Vui lòng nhập mật khẩu mới", Toast.LENGTH_SHORT).show()
                }
                confirmPassword.isEmpty() -> {
                    Toast.makeText(this, "Vui lòng xác nhận mật khẩu", Toast.LENGTH_SHORT).show()
                }
                newPassword.length < 8 -> {
                    Toast.makeText(this, "Mật khẩu phải có ít nhất 8 ký tự", Toast.LENGTH_SHORT).show()
                }
                !newPassword.any { it.isDigit() } -> {
                    Toast.makeText(this, "Mật khẩu phải chứa ít nhất 1 số", Toast.LENGTH_SHORT).show()
                }
                !newPassword.any { it.isUpperCase() } -> {
                    Toast.makeText(this, "Mật khẩu phải chứa ít nhất 1 chữ hoa", Toast.LENGTH_SHORT).show()
                }
                newPassword != confirmPassword -> {
                    Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    resetPassword(newPassword)
                }
            }
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun validatePasswordStrength(password: String) {
        // Kiểm tra độ dài
        if (password.length >= 8) {
            tvMinLength.setTextColor(Color.parseColor("#4CAF50"))
            tvMinLength.setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(this, android.R.drawable.checkbox_on_background),
                null, null, null
            )
        } else {
            tvMinLength.setTextColor(Color.parseColor("#757575"))
            tvMinLength.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        }

        // Kiểm tra có số
        if (password.any { it.isDigit() }) {
            tvHasNumber.setTextColor(Color.parseColor("#4CAF50"))
            tvHasNumber.setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(this, android.R.drawable.checkbox_on_background),
                null, null, null
            )
        } else {
            tvHasNumber.setTextColor(Color.parseColor("#757575"))
            tvHasNumber.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        }

        // Kiểm tra chữ hoa
        if (password.any { it.isUpperCase() }) {
            tvHasUpperCase.setTextColor(Color.parseColor("#4CAF50"))
            tvHasUpperCase.setCompoundDrawablesWithIntrinsicBounds(
                ContextCompat.getDrawable(this, android.R.drawable.checkbox_on_background),
                null, null, null
            )
        } else {
            tvHasUpperCase.setTextColor(Color.parseColor("#757575"))
            tvHasUpperCase.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        }
    }

    private fun resetPassword(newPassword: String) {
        btnConfirmReset.isEnabled = false

        if (oobCode.isNotEmpty()) {
            // Sử dụng oobCode từ email link
            mAuth.confirmPasswordReset(oobCode, newPassword)
                .addOnCompleteListener { task ->
                    btnConfirmReset.isEnabled = true

                    if (task.isSuccessful) {
                        showSuccessDialog()
                    } else {
                        AlertDialog.Builder(this)
                            .setTitle("❌ Lỗi")
                            .setMessage("Không thể đặt lại mật khẩu. Link có thể đã hết hạn.")
                            .setPositiveButton("OK") { dialog, _ ->
                                dialog.dismiss()
                                finish()
                            }
                            .show()
                    }
                }
        } else {
            // Đổi mật khẩu cho user đang đăng nhập
            val user = mAuth.currentUser
            if (user != null) {
                user.updatePassword(newPassword)
                    .addOnCompleteListener { task ->
                        btnConfirmReset.isEnabled = true

                        if (task.isSuccessful) {
                            showSuccessDialog()
                        } else {
                            AlertDialog.Builder(this)
                                .setTitle("❌ Lỗi")
                                .setMessage("Không thể cập nhật mật khẩu. Vui lòng đăng nhập lại.")
                                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                                .show()
                        }
                    }
            } else {
                btnConfirmReset.isEnabled = true
                Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("✅ Thành công")
            .setMessage("Mật khẩu của bạn đã được đặt lại thành công!\n\nBạn có thể đăng nhập với mật khẩu mới.")
            .setPositiveButton("Đăng nhập ngay") { _, _ ->
                // Quay về màn hình đăng nhập
                finish()
            }
            .setCancelable(false)
            .show()
    }
}