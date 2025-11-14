package com.example.quanlychitieu_finly

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.cloudinary.Cloudinary
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.concurrent.thread
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class AddCategoryFragment : Fragment() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var cardImagePreview: MaterialCardView
    private lateinit var ivCategoryImage: ImageView
    private lateinit var etCategoryName: TextInputEditText
    private lateinit var tilCategoryName: TextInputLayout
    private lateinit var rgCategoryType: RadioGroup
    private lateinit var rbExpense: MaterialRadioButton
    private lateinit var rbIncome: MaterialRadioButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnSave: MaterialButton
    private lateinit var progressBar: ProgressBar

    private var selectedImageUri: Uri? = null
    private var selectedResId: Int? = null

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var cloudinary: Cloudinary

    private val predefinedImages: List<Int> by lazy {
        R.drawable::class.java.fields
            .filter { it.name.startsWith("ic_category_") }
            .map { it.getInt(null) }
    }

    // Chọn ảnh từ thư viện
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            selectedResId = null
            ivCategoryImage.setImageURI(it)
            ivCategoryImage.clearColorFilter()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_category, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ánh xạ view
        toolbar = view.findViewById(R.id.toolbar)
        cardImagePreview = view.findViewById(R.id.cardImagePreview)
        ivCategoryImage = view.findViewById(R.id.ivCategoryImage)
        etCategoryName = view.findViewById(R.id.etCategoryName)
        tilCategoryName = view.findViewById(R.id.tilCategoryName)
        rgCategoryType = view.findViewById(R.id.rgCategoryType)
        rbExpense = view.findViewById(R.id.rbExpense)
        rbIncome = view.findViewById(R.id.rbIncome)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnSave = view.findViewById(R.id.btnSave)
        progressBar = ProgressBar(requireContext())

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        cloudinary = CloudinaryConfig.cloudinaryInstance

        toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

        cardImagePreview.setOnClickListener { showImagePickerDialog() }
        btnCancel.setOnClickListener { parentFragmentManager.popBackStack() }
        btnSave.setOnClickListener { saveCategory() }
    }

    // Dialog chọn: Ảnh có sẵn hoặc từ thư viện
    private fun showImagePickerDialog() {
        val options = arrayOf("Chọn ảnh có sẵn", "Chọn từ thư viện")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Chọn ảnh danh mục")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showPredefinedImageDialog()
                    1 -> pickImageLauncher.launch("image/*")
                }
            }
            .show()
    }

    // Hiển thị grid ảnh có sẵn
    private fun showPredefinedImageDialog() {
        val gridView = GridView(requireContext())
        gridView.numColumns = 3

        val adapter = object : BaseAdapter() {
            override fun getCount() = predefinedImages.size
            override fun getItem(position: Int) = predefinedImages[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val imageView = (convertView as? ImageView) ?: ImageView(requireContext())
                imageView.setImageResource(predefinedImages[position])
                imageView.layoutParams = AbsListView.LayoutParams(220, 220)
                imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
                return imageView
            }
        }
        gridView.adapter = adapter

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Chọn ảnh có sẵn")
            .setView(gridView)
            .setNegativeButton("Hủy", null)
            .create()

        dialog.show()

        gridView.setOnItemClickListener { _, _, position, _ ->
            selectedResId = predefinedImages[position]
            selectedImageUri = null
            ivCategoryImage.setImageResource(selectedResId!!)
            ivCategoryImage.clearColorFilter()
            dialog.dismiss()
        }
    }

    // Lưu danh mục vào Firebase + Cloudinary
    private fun saveCategory() {
        val name = etCategoryName.text.toString().trim()
        if (name.isEmpty()) {
            tilCategoryName.error = "Vui lòng nhập tên danh mục"
            return
        } else {
            tilCategoryName.error = null
        }

        val type = if (rbExpense.isChecked) "spending" else "income"
        val userId = auth.currentUser?.uid ?: return

        progressBar.visibility = View.VISIBLE

        thread {
            try {
                // Lấy ảnh bitmap từ uri hoặc resource
                val bitmap = when {
                    selectedImageUri != null -> MediaStore.Images.Media.getBitmap(
                        requireActivity().contentResolver,
                        selectedImageUri
                    )
                    selectedResId != null -> {
                        val drawable = ContextCompat.getDrawable(requireContext(), selectedResId!!)!!
                        (drawable as BitmapDrawable).bitmap
                    }
                    else -> {
                        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_categorys)!!
                        (drawable as BitmapDrawable).bitmap
                    }
                }

                // Nén ảnh
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                val inputStream = ByteArrayInputStream(baos.toByteArray())

                val folderPath = "users/$userId/$type/$name"
                val uploadParams = mapOf(
                    "folder" to folderPath,
                    "public_id" to "icon",
                    "overwrite" to true
                )

                // Upload lên Cloudinary
                val uploadResult = cloudinary.uploader().upload(inputStream, uploadParams)
                inputStream.close()

                val imageUrl = uploadResult["secure_url"].toString()
                val categoryDocRef = db.collection("users").document(userId)
                    .collection("categories").document()

                val categoryData = hashMapOf(
                    "id" to categoryDocRef.id,
                    "name" to name,
                    "type" to type,
                    "iconUrl" to imageUrl,
                    "totalAmount" to 0L,
//                    "createdAt" to System.currentTimeMillis()
                )

                // Lưu vào Firestore
                categoryDocRef.set(categoryData)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Thêm danh mục thành công!", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Lỗi khi lưu vào Firestore!", Toast.LENGTH_SHORT).show()
                    }

            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                requireActivity().runOnUiThread {
                    progressBar.visibility = View.GONE
                }
            }
        }
    }
}
