package Category

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.cloudinary.Cloudinary
import com.example.quanlychitieu_finly.CloudinaryConfig
import com.example.quanlychitieu_finly.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.concurrent.thread

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

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var cloudinary: Cloudinary

    private var selectedImageUri: Uri? = null
    private var selectedResId: Int? = null

    private var editId: String? = null
    private var oldIconUrl: String? = null
    private var oldType: String? = null

    private val predefinedImages: List<Int> by lazy {
        R.drawable::class.java.fields
            .filter { it.name.startsWith("ic_category_") }
            .map { it.getInt(null) }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            selectedResId = null
            Glide.with(requireContext()).load(it).into(ivCategoryImage)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_category, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initViews(view)
        initFirebase()

        toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
        cardImagePreview.setOnClickListener { showImagePickerDialog() }
        btnCancel.setOnClickListener { parentFragmentManager.popBackStack() }
        btnSave.setOnClickListener { saveCategory() }

        loadEditDataIfNeeded()
    }

    private fun initViews(view: View) {
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
    }

    private fun initFirebase() {
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        cloudinary = CloudinaryConfig.cloudinaryInstance
    }

    private fun loadEditDataIfNeeded() {
        arguments?.let { b ->
            editId = b.getString("id")
            val oldName = b.getString("name")
            oldType = b.getString("type")
            oldIconUrl = b.getString("iconUrl")

            etCategoryName.setText(oldName)

            if (oldType == "spending") rbExpense.isChecked = true
            else rbIncome.isChecked = true

            Glide.with(requireContext())
                .load(oldIconUrl)
                .error(R.drawable.ic_categorys)
                .into(ivCategoryImage)

            toolbar.title = "Cập nhật danh mục"
            btnSave.text = "Cập nhật"
        }
    }

    private fun saveCategory() {
        val name = etCategoryName.text.toString().trim()
        if (name.isEmpty()) {
            tilCategoryName.error = "Không được để trống"
            return
        }
        tilCategoryName.error = null

        if (editId == null) addNewCategory(name)
        else updateCategory(name)
    }

    private fun addNewCategory(name: String) {
        uploadAndSave(name, newId = null)
    }

    private fun updateCategory(name: String) {
        uploadAndSave(name, newId = editId)
    }

    private fun uploadAndSave(name: String, newId: String?) {
        val userId = auth.currentUser!!.uid
        val type = if (rbExpense.isChecked) "spending" else "income"

        val needUpload = selectedImageUri != null || selectedResId != null

        progressBar.visibility = View.VISIBLE

        thread {
            try {

                val finalImageUrl = if (needUpload) {
                    // xử lý ảnh Cloudinary
                    val bitmap = when {
                        selectedImageUri != null ->
                            MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, selectedImageUri)

                        selectedResId != null -> {
                            val drawable = ContextCompat.getDrawable(requireContext(), selectedResId!!)!!
                            (drawable as BitmapDrawable).bitmap
                        }

                        else -> null
                    }

                    val baos = ByteArrayOutputStream()
                    bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, baos)
                    val inputStream = ByteArrayInputStream(baos.toByteArray())

                    val uploadResult = cloudinary.uploader().upload(inputStream, mapOf(
                        "folder" to "users/$userId/categories",
                        "overwrite" to true
                    ))

                    uploadResult["secure_url"].toString()

                } else {
                    oldIconUrl ?: ""
                }

                val id = newId ?: db.collection("tmp").document().id

                val data = mapOf(
                    "id" to id,
                    "name" to name,
                    "type" to type,
                    "iconUrl" to finalImageUrl
                )

                db.collection("users")
                    .document(userId)
                    .collection("categories")
                    .document(id)
                    .update(data)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Thành công", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                    .addOnFailureListener {
                        db.collection("users")
                            .document(userId)
                            .collection("categories")
                            .document(id)
                            .set(data)
                        Toast.makeText(context, "Thành công", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
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

    private fun showImagePickerDialog() {
        val options = arrayOf("Ảnh có sẵn", "Ảnh từ thư viện")
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
            .setView(gridView)
            .setNegativeButton("Đóng", null)
            .create()

        dialog.show()

        gridView.setOnItemClickListener { _, _, position, _ ->
            selectedResId = predefinedImages[position]
            selectedImageUri = null
            ivCategoryImage.setImageResource(selectedResId!!)
            dialog.dismiss()
        }
    }
}
