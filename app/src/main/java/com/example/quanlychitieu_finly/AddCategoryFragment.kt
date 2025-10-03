package com.example.quanlychitieu_finly

import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

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

    private var selectedImageUri: Uri? = null
    private var selectedResId: Int? = null

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

        // Toolbar quay lại
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Nhấn vào ảnh -> chọn ảnh
        cardImagePreview.setOnClickListener {
            showImagePickerDialog()
        }

        // Nút Hủy
        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Nút Lưu
        btnSave.setOnClickListener {
            saveCategory()
        }
    }

    // Dialog chọn: Ảnh có sẵn hoặc từ thư viện
    private fun showImagePickerDialog() {
        val options = arrayOf("Chọn ảnh có sẵn", "Chọn từ thư viện")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Chọn ảnh danh mục")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showPredefinedImageDialog() // mở grid ảnh có sẵn
                    1 -> pickImageLauncher.launch("image/*")
                }
            }
            .show()
    }

    // Hiển thị dialog grid ảnh có sẵn
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

    // Validate & lưu
    private fun saveCategory() {
        val name = etCategoryName.text.toString().trim()
        if (name.isEmpty()) {
            tilCategoryName.error = "Vui lòng nhập tên danh mục"
            return
        } else {
            tilCategoryName.error = null
        }

        if (selectedImageUri == null && selectedResId == null) {
            Toast.makeText(requireContext(), "Vui lòng chọn ảnh", Toast.LENGTH_SHORT).show()
            return
        }

        val type = if (rbExpense.isChecked) "Chi tiêu" else "Thu nhập"

        // Xử lý thêm vào DB sau này, hiện tại chỉ Toast
        Toast.makeText(
            requireContext(),
            "Đã thêm danh mục: $name ($type)",
            Toast.LENGTH_LONG
        ).show()

        parentFragmentManager.popBackStack()
    }
}
