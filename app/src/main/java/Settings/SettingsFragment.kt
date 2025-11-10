package Settings

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.cloudinary.Cloudinary
import com.example.quanlychitieu_finly.CloudinaryConfig
import com.example.quanlychitieu_finly.LoginActivity
import com.example.quanlychitieu_finly.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import android.widget.LinearLayout
import android.widget.EditText
import androidx.appcompat.app.AppCompatDelegate



class SettingsFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var cloudinary: Cloudinary

    // Views
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var imgAvatar: ImageView
    private val imagePickRequest = 1001

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Khởi tạo Firebase và Cloudinary
        sharedPreferences = requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        cloudinary = CloudinaryConfig.cloudinaryInstance

        // Ánh xạ View
        tvUserName = view.findViewById(R.id.tvUserName)
        tvUserEmail = view.findViewById(R.id.tvUserEmail)
        imgAvatar = view.findViewById(R.id.imgAvatar)


        // Tải thông tin người dùng
        loadUserInfo()

        // Khi nhấn vào card hoặc avatar -> chọn ảnh
        view.findViewById<CardView>(R.id.cardUserProfile).setOnClickListener { openImagePicker() }
        imgAvatar.setOnClickListener { openImagePicker() }

        // Nút đăng xuất
        view.findViewById<CardView>(R.id.btnLogout).setOnClickListener { showLogoutDialog() }
        view.findViewById<LinearLayout>(R.id.btnChangePassword).setOnClickListener {
            showChangePasswordDialog()
        }
        view.findViewById<LinearLayout>(R.id.tvCurrentTheme).setOnClickListener {
            showChangePasswordDialog()
        }
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)



        return view
    }

    private fun showChangePasswordDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_change_password)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val edtOldPass = dialog.findViewById<EditText>(R.id.etOldPassword)
        val edtNewPass = dialog.findViewById<EditText>(R.id.etNewPassword)
        val edtConfirmPass = dialog.findViewById<EditText>(R.id.etConfirmPassword)
        val btnCancel = dialog.findViewById<CardView>(R.id.btnCancelPassword)
        val btnConfirm = dialog.findViewById<CardView>(R.id.btnSavePassword)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            val oldPass = edtOldPass.text.toString().trim()
            val newPass = edtNewPass.text.toString().trim()
            val confirmPass = edtConfirmPass.text.toString().trim()

            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass != confirmPass) {
                Toast.makeText(requireContext(), "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass.length < 8) {
                Toast.makeText(requireContext(), "Mật khẩu phải có ít nhất 8 ký tự", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            changeUserPassword(oldPass, newPass, dialog)
        }


        dialog.show()
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels - (40 * metrics.density).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    private fun changeUserPassword(oldPassword: String, newPassword: String, dialog: Dialog) {
        val user = auth.currentUser
        val email = user?.email

        if (user == null || email.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Người dùng chưa đăng nhập", Toast.LENGTH_SHORT).show()
            return
        }

        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, oldPassword)

        // Xác thực lại trước khi đổi mật khẩu
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Lỗi khi đổi mật khẩu: ${it.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Mật khẩu hiện tại không đúng", Toast.LENGTH_SHORT).show()
            }
    }



    /** Mở thư viện chọn ảnh */
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, imagePickRequest)
    }

    /** Nhận kết quả ảnh chọn từ thư viện */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == imagePickRequest && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data ?: return
            uploadNewAvatar(imageUri)
        }
    }

    /** Upload ảnh đại diện mới lên Cloudinary + cập nhật Firestore */
    private fun uploadNewAvatar(imageUri: Uri) {
        val userId = auth.currentUser?.uid ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Đọc ảnh từ Uri
                val inputStream = requireContext().contentResolver.openInputStream(imageUri)
                    ?: throw Exception("Không thể đọc file ảnh")

                // Upload lên Cloudinary (xóa cache ảnh cũ)
                val uploadParams = mapOf(
                    "folder" to "users/$userId/avt",
                    "public_id" to "avatar_${System.currentTimeMillis()}",
                    "overwrite" to true,
                    "invalidate" to true
                )
                val uploadResult = cloudinary.uploader().upload(inputStream, uploadParams)
                inputStream.close()

                // Lấy link HTTPS an toàn từ Cloudinary
                val secureUrl = uploadResult["secure_url"]?.toString()
                    ?: throw Exception("Không nhận được URL từ Cloudinary")

                // Cập nhật Firestore (dù document chưa có vẫn tạo mới)
                val userRef = db.collection("users").document(userId)
                val snapshot = userRef.get().await()
                if (snapshot.exists()) {
                    userRef.update("avatarUrl", secureUrl).await()
                } else {
                    userRef.set(mapOf("avatarUrl" to secureUrl), SetOptions.merge()).await()
                }

                // Lưu vào SharedPreferences
                sharedPreferences.edit().putString("user_avatar", secureUrl).apply()

                // Cập nhật UI (load ảnh mới, bỏ cache)
                withContext(Dispatchers.Main) {
                    Glide.with(requireContext())
                        .load("$secureUrl?v=${System.currentTimeMillis()}") // cache buster
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .placeholder(R.drawable.ic_loading)
                        .error(R.drawable.ic_loading)
                        .into(imgAvatar)

                    Toast.makeText(requireContext(), "Ảnh đại diện đã được cập nhật!", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Lỗi cập nhật ảnh: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /** Tải thông tin người dùng từ Firestore */
    private fun loadUserInfo() {
        val currentUser = auth.currentUser
        val userId = currentUser?.uid

        if (userId != null) {
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val username = document.getString("username") ?: "Người dùng"
                        val email = document.getString("email") ?: currentUser.email ?: ""
                        val avatarUrl = document.getString("avatarUrl") ?: ""

                        tvUserName.text = username
                        tvUserEmail.text = email

                        if (avatarUrl.isNotEmpty()) {
                            Glide.with(requireContext())
                                .load("$avatarUrl?v=${System.currentTimeMillis()}") // tránh cache cũ
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .skipMemoryCache(true)
                                .placeholder(R.drawable.ic_loading)
                                .error(R.drawable.ic_user_placeholder)
                                .into(imgAvatar)
                        } else {
                            imgAvatar.setImageResource(R.drawable.ic_user_placeholder)
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Lỗi tải thông tin người dùng", Toast.LENGTH_SHORT).show()
                }
        } else {
            tvUserName.text = "Khách"
            tvUserEmail.text = "Chưa đăng nhập"
            imgAvatar.setImageResource(R.drawable.ic_user_placeholder)
        }
    }

    private fun showLogoutDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_logout)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.findViewById<TextView>(R.id.btnCancelLogout).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<MaterialButton>(R.id.btnConfirmLogout).setOnClickListener {
            auth.signOut()
            Toast.makeText(requireContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show()
            dialog.dismiss()

            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        dialog.show()
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels - (40 * metrics.density).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
