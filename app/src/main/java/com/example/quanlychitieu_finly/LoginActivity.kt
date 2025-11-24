package com.example.quanlychitieu_finly

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

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

        // Hiện / ẩn mật khẩu
        cbShowPassword.setOnCheckedChangeListener { _, checked ->
            val start = edtPassword.selectionStart
            val end = edtPassword.selectionEnd
            edtPassword.transformationMethod =
                if (checked) null else PasswordTransformationMethod.getInstance()
            edtPassword.setSelection(start.coerceAtLeast(0), end.coerceAtLeast(0))
        }

        // Nhấn DONE trên bàn phím để login
        edtPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin()
                true
            } else false
        }

        btnLogin.setOnClickListener { attemptLogin() }

        txtGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        txtForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    // =====================================================================
    // 1. XỬ LÝ ĐĂNG NHẬP + 2FA DEVICE/IP
    // =====================================================================

    private fun attemptLogin() {
        val email = edtEmail.text.toString().trim()
        val password = edtPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ email và mật khẩu", Toast.LENGTH_SHORT).show()
            return
        }

        btnLogin.isEnabled = false

        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                btnLogin.isEnabled = true

                if (!task.isSuccessful) {
                    Toast.makeText(this, "Sai email hoặc mật khẩu!", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }

                val uid = mAuth.currentUser?.uid ?: return@addOnCompleteListener
                val db = FirebaseFirestore.getInstance()

                // Lấy thông tin thiết bị + IP hiện tại
                val currentDeviceId = getMyDeviceId()
                val currentIP = getLocalIpAddress()

                db.collection("users").document(uid)
                    .get()
                    .addOnSuccessListener { doc ->
                        if (!doc.exists()) {
                            // Phòng trường hợp tài khoản chưa có document user
                            proceedToHome()
                            return@addOnSuccessListener
                        }

                        val twoFAEnabled = doc.getBoolean("twoFAEnabled") ?: false
                        val lastDeviceId = doc.getString("lastDeviceId")
                        val lastIP = doc.getString("lastIP")

                        // Nếu chưa bật 2FA → vào thẳng
                        if (!twoFAEnabled) {
                            updateLoginInfo(uid, currentDeviceId, currentIP)
                            proceedToHome()
                            return@addOnSuccessListener
                        }

                        // Người dùng đã bật 2FA
                        val isNewDevice = !lastDeviceId.isNullOrEmpty() && lastDeviceId != currentDeviceId
                        val isNewIP = !lastIP.isNullOrEmpty() && lastIP != currentIP

                        if (!isNewDevice && !isNewIP) {
                            // Thiết bị & IP giống lần trước → không cần OTP
                            updateLoginInfo(uid, currentDeviceId, currentIP)
                            proceedToHome()
                        } else {
                            // Thiết bị/IP lạ → gửi OTP + yêu cầu xác minh
                            sendOtpCode(uid) {
                                showOtpVerificationDialog(uid, currentDeviceId, currentIP)
                            }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Lỗi tải dữ liệu người dùng!", Toast.LENGTH_SHORT)
                            .show()
                    }
            }
    }

    // =====================================================================
    // 2. OTP: GỬI & XÁC MINH
    // =====================================================================

    /**
     * Gửi OTP (demo): tạo mã 6 số, lưu Firestore, hiện Toast + Log.
     * Thực tế bạn thay bằng gửi Email/SMS.
     */
    private fun sendOtpCode(uid: String, onDone: () -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val otp = Random.nextInt(100000, 999999).toString()
        val expireAt = System.currentTimeMillis() + 5 * 60_000 // 5 phút

        val userEmail = edtEmail.text.toString().trim()

        val data = hashMapOf<String, Any>(
            "pendingOtpCode" to otp,
            "pendingOtpExpireAt" to expireAt
        )

        db.collection("users").document(uid)
            .update(data)
            .addOnSuccessListener {

                EmailSender.sendEmail(
                    toEmail = userEmail,
                    subject = "Mã OTP đăng nhập Finly",
                    message = "Mã OTP của bạn là: $otp\nCó hiệu lực trong 5 phút."
                ) { success ->

                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this, "OTP đã gửi đến email!", Toast.LENGTH_SHORT).show()
                            onDone()
                        } else {
                            Toast.makeText(this, "Không gửi được OTP!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
    }

    /**
     * Dialog nhập OTP.
     */
    private fun showOtpVerificationDialog(uid: String, newDeviceId: String, newIP: String) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_verify_otp)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val edtOtp = dialog.findViewById<EditText>(R.id.edtOtp)
        val btnVerify = dialog.findViewById<Button>(R.id.btnVerifyOtp)
        val btnCancel = dialog.findViewById<TextView>(R.id.btnCancelOtp)

        btnCancel.setOnClickListener {
            dialog.dismiss()
            // Có thể signOut để buộc đăng nhập lại nếu bạn muốn
            // mAuth.signOut()
        }

        btnVerify.setOnClickListener {
            val inputCode = edtOtp.text.toString().trim()
            if (inputCode.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập mã OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            verifyOtpCode(uid, inputCode, dialog, newDeviceId, newIP)
        }

        dialog.show()

        val window = dialog.window
        val params = window?.attributes
        val displayMetrics = resources.displayMetrics

        params?.width = (displayMetrics.widthPixels * 0.9).toInt()   // 90%
        window?.attributes = params
    }

    /**
     * Kiểm tra OTP với Firestore.
     */
    private fun verifyOtpCode(
        uid: String,
        inputCode: String,
        dialog: Dialog,
        newDeviceId: String,
        newIP: String
    ) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val realCode = doc.getString("pendingOtpCode")
                val expireAt = doc.getLong("pendingOtpExpireAt") ?: 0L
                val now = System.currentTimeMillis()

                if (realCode.isNullOrEmpty() || now > expireAt) {
                    Toast.makeText(this, "Mã OTP đã hết hạn, vui lòng đăng nhập lại!", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                    return@addOnSuccessListener
                }

                if (inputCode != realCode) {
                    Toast.makeText(this, "Mã OTP không chính xác!", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // OTP hợp lệ → cập nhật device/IP mới + clear OTP
                val updates = hashMapOf<String, Any>(
                    "lastDeviceId" to newDeviceId,
                    "lastIP" to newIP,
                    "lastLoginAt" to now,
                    "pendingOtpCode" to "",
                    "pendingOtpExpireAt" to 0L
                )

                db.collection("users").document(uid)
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Xác minh thành công!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        proceedToHome()
                    }
                    .addOnFailureListener { e ->
                        Log.w("Login", "Cập nhật dữ liệu sau OTP thất bại", e)
                        Toast.makeText(
                            this,
                            "Lỗi hệ thống, vui lòng thử lại!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi kiểm tra OTP!", Toast.LENGTH_SHORT).show()
            }
    }

    // =====================================================================
    // 3. HÀM TIỆN ÍCH: DEVICE ID, IP, CẬP NHẬT LOGIN, ĐI VÀO MAIN
    // =====================================================================

    /**
     * Lấy Device ID riêng của máy (ANDROID_ID).
     * Đổi tên thành getMyDeviceId() để không trùng hàm hệ thống.
     */
    private fun getMyDeviceId(): String {
        return android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
    }

    /**
     * Lấy IPv4 cục bộ (Wi-Fi / Mobile).
     */
    private fun getLocalIpAddress(): String {
        return try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val inet = addrs.nextElement()
                    if (!inet.isLoopbackAddress && inet is java.net.Inet4Address) {
                        return inet.hostAddress ?: ""
                    }
                }
            }
            ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Ghi lại thông tin login vào Firestore: thiết bị, IP, thời điểm.
     */
    private fun updateLoginInfo(uid: String, deviceId: String, ip: String) {
        val db = FirebaseFirestore.getInstance()
        val updates = hashMapOf<String, Any>(
            "lastDeviceId" to deviceId,
            "lastIP" to ip,
            "lastLoginAt" to System.currentTimeMillis()
        )

        db.collection("users").document(uid)
            .update(updates)
            .addOnFailureListener { e ->
                Log.w("Login", "updateLoginInfo failed", e)
            }
    }

    /**
     * Mở màn MainActivity sau khi login OK.
     */
    private fun proceedToHome() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
