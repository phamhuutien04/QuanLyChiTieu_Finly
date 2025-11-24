package com.example.quanlychitieu_finly

import Category.Category
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.util.Base64
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.Normalizer
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    companion object {
        private const val TAG_OCR = "OCR"
        private const val TAG_PARSER = "OCR_PARSER"

        // OCR.space key của bạn
        private const val OCR_SPACE_API_KEY = "K84179895088957"

        // OkHttpClient tái sử dụng cho toàn fragment
        private val httpClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS)
            .writeTimeout(40, TimeUnit.SECONDS)
            .build()
    }

    // Firebase
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // UI chính
    private lateinit var rvTransactions: RecyclerView
    private lateinit var adapter: TransactionAdapter
    private val transactionList = mutableListOf<Transaction>()

    private var tvIncome: TextView? = null
    private var tvExpense: TextView? = null
    private var tvBalance: TextView? = null
    private var tvCount: TextView? = null

    // OCR invoice
    private var invoiceImageUri: Uri? = null

    // Các input trong BottomSheet
    private lateinit var edtTitle: TextInputEditText
    private lateinit var edtCategory: TextInputEditText
    private lateinit var edtAmount: TextInputEditText

    // Launcher chụp ảnh hóa đơn
    private val takeInvoicePhotoLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && invoiceImageUri != null) {
                sendInvoiceToOcrSpace(invoiceImageUri!!)
            }
        }

    // ======================= LIFECYCLE =======================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        if (!ensureLoggedIn()) return view

        bindUserHeader(view)
        setupHomeUi(view)
        loadRecentTransactions()
        setupViewAllButton(view)

        return view
    }

    override fun onResume() {
        super.onResume()
        if (ensureLoggedIn()) {
            view?.let { bindUserHeader(it) }
            loadRecentTransactions()
        }
    }

    // ======================= AUTH =======================

    private fun ensureLoggedIn(): Boolean {
        val user = auth.currentUser
        return if (user == null) {
            redirectToLogin()
            false
        } else true
    }

    private fun redirectToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        }
        startActivity(intent)
        requireActivity().finish()
    }

    // ======================= UI CHÍNH =======================

    private fun setupHomeUi(root: View) {
        rvTransactions = root.findViewById(R.id.rvTransactions)
        adapter = TransactionAdapter(transactionList)
        rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        rvTransactions.adapter = adapter

        val btnAddIncome: MaterialCardView = root.findViewById(R.id.add_income_layout)
        val btnAddExpense: MaterialCardView = root.findViewById(R.id.add_expense_layout)
        val btnReports: MaterialCardView = root.findViewById(R.id.btnReports)

        btnAddIncome.setOnClickListener { openAddTransactionFlow("income") }
        btnAddExpense.setOnClickListener { openAddTransactionFlow("spending") }
        btnReports.setOnClickListener { (activity as? MainActivity)?.switchToAnalyticsTab() }

        tvIncome = root.findViewById(R.id.tvIncome)
        tvExpense = root.findViewById(R.id.tvExpense)
        tvBalance = root.findViewById(R.id.tvBalance)
        tvCount = root.findViewById(R.id.tvTransactionCount)

        val tvDate = root.findViewById<TextView>(R.id.tvDate)
        val df = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("vi", "VN"))
        tvDate.text = df.format(Date())
    }

    private fun bindUserHeader(root: View) {
        val tvUserName = root.findViewById<TextView>(R.id.tvUserName)
        val imgAvatar = root.findViewById<ImageView>(R.id.imgAvatar)
        val user = auth.currentUser ?: return

        tvUserName.text = bestNameFromAuth(user.displayName, user.email)

        // 1) Load avatar từ FirebaseAuth nếu có
        val authAvatar = user.photoUrl?.toString()
        if (!authAvatar.isNullOrEmpty()) {
            Glide.with(requireContext())
                .load(authAvatar)
                .placeholder(R.drawable.ic_user_placeholder)
                .error(R.drawable.ic_user_placeholder)
                .into(imgAvatar)
        } else {
            imgAvatar.setImageResource(R.drawable.ic_user_placeholder)
        }

        // 2) Load avatar custom từ Firestore
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val fullName = doc.getString("fullName")
                if (!fullName.isNullOrEmpty()) tvUserName.text = fullName

                val avatarUrl = doc.getString("avatarUrl")

                if (!avatarUrl.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(avatarUrl)
                        .placeholder(R.drawable.ic_user_placeholder)
                        .error(R.drawable.ic_user_placeholder)
                        .into(imgAvatar)
                } else {
                    imgAvatar.setImageResource(R.drawable.ic_user_placeholder)
                }
            }
    }

    // ======================= BOTTOM SHEET THÊM GIAO DỊCH =======================

    private fun openAddTransactionFlow(type: String) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("categories")
            .whereEqualTo("type", type)
            .orderBy("name")
            .get()
            .addOnSuccessListener { snap ->
                val categories = snap.documents.mapNotNull { doc ->
                    doc.toObject(Category::class.java)?.apply { id = doc.id }
                }

                if (categories.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Chưa có danh mục $type",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }

                showAddTransactionSheet(type, categories)
            }
            .addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    "Lỗi tải danh mục: ${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun showAddTransactionSheet(type: String, categories: List<Category>) {
        val dialog = BottomSheetDialog(
            requireContext(),
            com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog
        )
        val v = layoutInflater.inflate(R.layout.bottom_sheet_add_transaction, null)
        dialog.setContentView(v)

        val tvTitle = v.findViewById<TextView>(R.id.tvSheetTitle)
        val btnSave = v.findViewById<MaterialButton>(R.id.btnSave)
        val btnCancel = v.findViewById<MaterialButton>(R.id.btnCancel)
        val btnQr = v.findViewById<ImageButton>(R.id.btnImageQr)

        edtTitle = v.findViewById(R.id.edtTitle)
        edtCategory = v.findViewById(R.id.edtCategory)
        edtAmount = v.findViewById(R.id.edtAmount)

        tvTitle.text = if (type == "income") "Thêm thu nhập" else "Thêm chi tiêu"

        // ===============================
        // 🔥 KIỂM TRA CÓ CHO PHÉP SCAN OCR HAY KHÔNG
        // ===============================
        val prefs = requireContext().getSharedPreferences("FinlyPrefs", Context.MODE_PRIVATE)
        val isScanEnabled = prefs.getBoolean("scan_invoice_enabled", false)

        btnQr.visibility = if (isScanEnabled) View.VISIBLE else View.GONE
        // ===============================

        var chosen: Category? = categories.firstOrNull()
        edtCategory.setText(chosen?.name ?: "")

        // Chọn danh mục
        edtCategory.setOnClickListener {
            val catList = categories.sortedBy { it.name.lowercase(Locale.ROOT) }
            val pickDialog = BottomSheetDialog(
                requireContext(),
                com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog
            )
            val pickView =
                layoutInflater.inflate(R.layout.bottom_sheet_category_picker, null)
            pickDialog.setContentView(pickView)

            val rv = pickView.findViewById<RecyclerView>(R.id.rvCategories)
            rv.layoutManager = LinearLayoutManager(requireContext())
            rv.adapter =
                CategoryPickerAdapter(catList.map { CatUI(it.id, it.name, it.iconUrl ?: "") }) {
                    chosen = categories.find { c -> c.id == it.id }
                    edtCategory.setText(it.name)
                    pickDialog.dismiss()
                }

            pickView.findViewById<MaterialButton>(R.id.btnCancel)
                ?.setOnClickListener { pickDialog.dismiss() }

            pickDialog.show()
        }

        // Định dạng tiền
        setupMoneyFormatter(edtAmount)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val title = edtTitle.text?.toString()?.trim().orEmpty()
            val cleanAmount = edtAmount.text?.toString()
                ?.replace(".", "")
                ?.replace(",", "")
                ?.trim()
                .orEmpty()
            val amount = cleanAmount.toDoubleOrNull() ?: 0.0

            if (title.isEmpty() || chosen == null || amount <= 0) {
                Toast.makeText(
                    requireContext(),
                    "Vui lòng nhập đủ thông tin hợp lệ",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            saveTransaction(type, title, chosen!!, amount) { savedTx ->
                showTransactionSuccessSheet(savedTx)
            }

            dialog.dismiss()
        }

        // Nút chụp hóa đơn -> OCR -> tự điền
        btnQr.setOnClickListener { openInvoiceCamera() }

        dialog.show()
    }

    private fun setupMoneyFormatter(editText: TextInputEditText) {
        val vnFormat = NumberFormat.getInstance(Locale("vi", "VN"))
        editText.keyListener = DigitsKeyListener.getInstance("0123456789.,")
        editText.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(18))

        editText.addTextChangedListener(object : TextWatcher {
            var isFormatting = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                val raw = s?.toString() ?: return

                val digits = raw.replace(".", "")
                    .replace(",", "")
                    .replace("[^0-9]".toRegex(), "")

                if (digits.isEmpty()) {
                    isFormatting = true
                    editText.setText("")
                    isFormatting = false
                    return
                }

                val parsed = digits.toDoubleOrNull() ?: return
                val formatted = vnFormat.format(parsed)

                if (formatted != raw) {
                    isFormatting = true
                    editText.setText(formatted)
                    editText.setSelection(formatted.length)
                    isFormatting = false
                }
            }
        })
    }

    // ======================= OCR SPACE =======================

    private fun openInvoiceCamera() {
        val file = File(
            requireContext().cacheDir,
            "invoice_${System.currentTimeMillis()}.jpg"
        )
        invoiceImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )
        takeInvoicePhotoLauncher.launch(invoiceImageUri)
    }

    private fun compressImage(uri: Uri, maxSizeBytes: Int = 900_000): ByteArray {
        val bitmap =
            MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)

        var quality = 100
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)

        while (stream.size() > maxSizeBytes && quality > 10) {
            quality -= 10
            stream.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        }

        return stream.toByteArray()
    }

    private fun sendInvoiceToOcrSpace(imageUri: Uri) {
        val inputStream = requireContext().contentResolver.openInputStream(imageUri)
        if (inputStream == null) {
            showToast("Không thể đọc ảnh")
            return
        }

        val imageBytes = compressImage(imageUri)
        val base64Image = "data:image/jpeg;base64," +
                Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        val formBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("base64Image", base64Image)
            .addFormDataPart("language", "vnm")
            .addFormDataPart("isOverlayRequired", "false")
            .addFormDataPart("scale", "true")
            .addFormDataPart("isTable", "true")
            .addFormDataPart("OCREngine", "2")
            .build()

        val request = Request.Builder()
            .url("https://api.ocr.space/parse/image")
            .addHeader("apikey", OCR_SPACE_API_KEY)
            .post(formBody)
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Lỗi mạng: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val raw = response.body?.string() ?: ""
                Log.e(TAG_OCR, "RAW: $raw")

                try {
                    val json = JSONObject(raw)

                    if (json.optBoolean("IsErroredOnProcessing", false)) {
                        val msg = json.optString("ErrorMessage", "Không rõ lỗi")
                        showToast("OCR lỗi: $msg")
                        return
                    }

                    val parsedResults = json.optJSONArray("ParsedResults")
                    if (parsedResults == null || parsedResults.length() == 0) {
                        showToast("Không tìm thấy kết quả OCR")
                        return
                    }

                    val page = parsedResults.getJSONObject(0)
                    val exitCode = page.optInt("FileParseExitCode", -99)
                    if (exitCode != 1) {
                        val detail = page.optString("ErrorDetails", "Không rõ")
                        showToast("OCR thất bại: $detail")
                        return
                    }

                    val parsedText = page.optString("ParsedText", "").trim()
                    if (parsedText.isBlank()) {
                        showToast("Không đọc được văn bản")
                        return
                    }

                    Log.e(TAG_OCR, "TEXT:\n$parsedText")

                    // Parse thông minh 3 thông tin
                    val result = parseOcrToTransaction(parsedText)
                    Log.e(TAG_PARSER, "Parsed: $result")

                    requireActivity().runOnUiThread {
                        edtTitle.setText(result.title)
                        edtCategory.setText(result.category)
                        edtAmount.setText(result.amount.toString())

                        Toast.makeText(
                            requireContext(),
                            "Đã tự động điền từ hóa đơn",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    showToast("Lỗi phân tích JSON OCR")
                }
            }
        })
    }

    // ======================= OCR PARSER (THÔNG MINH) =======================

    private data class OcrParsedResult(
        val title: String,
        val category: String,
        val amount: Long
    )

    private fun parseOcrToTransaction(rawText: String): OcrParsedResult {
        val normalized = normalizeOcrText(rawText)

        val amount = extractFinalAmount(normalized)
        val category = detectCategory(normalized)
        val title = extractTitle(normalized, category)

        return OcrParsedResult(
            title = title,
            category = category,
            amount = amount
        )
    }

    /**
     * Chuẩn hóa text: gom khoảng trắng, giữ line-break hợp lý
     */
    private fun normalizeOcrText(text: String): String {
        return text
            .replace("\r", "\n")
            .replace("\t", " ")
            .replace(Regex(" +"), " ")
            .replace(Regex("\n{2,}"), "\n")
            .trim()
    }

    /**
     * Lấy số tiền cuối cùng của hóa đơn.
     * Ưu tiên dòng chứa "Tiền Thanh Toán", "Tổng tiền (VAT)", nếu không có thì lấy số cuối cùng.
     */
    private fun extractFinalAmount(text: String): Long {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // Ưu tiên tìm trong các dòng quan trọng
        val importantKeywords = listOf(
            "tiền thanh toán",
            "tong tien",
            "tổng tiền",
            "total"
        )

        val numberRegex = Regex("\\b[0-9][0-9 .]{2,}\\b")

        val importantLine = lines.lastOrNull { line ->
            val lower = line.lowercase(Locale.ROOT)
            importantKeywords.any { lower.contains(it) }
        }

        val candidateFromImportant = importantLine?.let { line ->
            numberRegex.findAll(line).map { it.value }.lastOrNull()
        }

        val rawAmount = candidateFromImportant
            ?: run {
                // Nếu không thấy trong dòng quan trọng -> lấy số cuối cùng trong toàn bộ text
                val allNumbers = numberRegex.findAll(text).map { it.value }.toList()
                if (allNumbers.isNotEmpty()) allNumbers.last() else "0"
            }

        val clean = rawAmount
            .replace(" ", "")
            .replace(".", "")
            .replace(",", "")

        return clean.toLongOrNull() ?: 0L
    }

    /**
     * Đoán danh mục dựa vào nội dung.
     */
    private fun detectCategory(text: String): String {
        val lower = text.lowercase(Locale.ROOT)

        return when {
            listOf("cafe", "coffee", "tea", "milk", "tra ", "trà ", "food", "drink").any {
                lower.contains(it)
            } -> "An uong"

            listOf("grab", "gojek", "be ", "taxi", "bus", "xe khach", "train").any {
                lower.contains(it)
            } -> "Di chuyen"

            listOf("điện", "dien", "nuoc", "nước", "internet", "wifi", "hoa don", "hóa đơn").any {
                lower.contains(it)
            } -> "Hoa don"

            listOf("gym", "fitness", "sport", "stadium", "court").any {
                lower.contains(it)
            } -> "The thao"

            listOf("clinic", "benh vien", "bệnh viện", "phong kham", "thuoc", "thuốc").any {
                lower.contains(it)
            } -> "Y te"

            listOf("shopee", "tiki", "lazada", "winmart", "vinmart", "supermarket").any {
                lower.contains(it)
            } -> "Mua sam"

            listOf("cinema", "rap phim", "phim", "karaoke", "game center").any {
                lower.contains(it)
            } -> "Giai tri"

            else -> "Khac"
        }
    }

    /**
     * Tạo tiêu đề đơn giản, không dấu.
     * Ưu tiên tên quán hoặc dòng có "cafe/coffee/..." nếu có, fallback là dòng đầu.
     */
    private fun extractTitle(text: String, category: String): String {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        val candidate = lines.firstOrNull { line ->
            val lower = line.lowercase(Locale.ROOT)
            listOf("cafe", "coffee", "shop", "restaurant", "quan ").any {
                lower.contains(it)
            }
        } ?: lines.firstOrNull { line ->
            // bỏ qua các từ chung chung
            val lower = line.lowercase(Locale.ROOT)
            !lower.contains("phiếu thanh toán") &&
                    !lower.contains("phieu thanh toan") &&
                    !lower.contains("hoa don") &&
                    !lower.contains("invoice")
        } ?: "Giao dich $category"

        val noAccent = removeVietnameseAccent(candidate)
        return noAccent.lowercase(Locale.ROOT)
    }

    private fun removeVietnameseAccent(input: String): String {
        var result = Normalizer.normalize(input, Normalizer.Form.NFD)
        result = result.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        result = result.replace("Đ", "D").replace("đ", "d")
        return result
    }

    // ======================= LƯU GIAO DỊCH & SUMMARY =======================

    private fun saveTransaction(
        type: String,
        title: String,
        category: Category,
        amount: Double,
        onSuccess: (Transaction) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return
        val userDoc = db.collection("users").document(userId)
        val txCol = userDoc.collection("transactions")
        val txId = txCol.document().id

        val tx = Transaction(
            title = title,
            categoryId = category.id,
            categoryName = category.name,
            categoryIconUrl = category.iconUrl,
            amount = amount,
            type = type,
            date = Timestamp.now()
        )

        db.runBatch { batch ->
            batch.set(txCol.document(txId), tx)
            batch.update(
                userDoc.collection("categories").document(category.id),
                "totalAmount",
                com.google.firebase.firestore.FieldValue.increment(amount)
            )
        }.addOnSuccessListener {
            loadRecentTransactions()

            if (type == "spending") {
                checkGoalExceeded { onSuccess(tx) }
            } else {
                onSuccess(tx)
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadRecentTransactions() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("transactions")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(3)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                transactionList.clear()
                snapshot?.forEach { doc ->
                    transactionList.add(doc.toObject(Transaction::class.java))
                }
                adapter.notifyDataSetChanged()
                tvCount?.text = "${transactionList.size} giao dịch gần đây"
            }

        calculateTotals(userId)
    }

    private fun calculateTotals(userId: String) {
        db.collection("users").document(userId)
            .collection("transactions")
            .addSnapshotListener { snapshot, _ ->
                var income = 0.0
                var expense = 0.0

                snapshot?.forEach { doc ->
                    val t = doc.toObject(Transaction::class.java)
                    if (t.type == "income") income += t.amount else expense += t.amount
                }

                tvIncome?.text = formatVnd(income)
                tvExpense?.text = formatVnd(expense)
                tvBalance?.text = formatVnd(income - expense)
            }
    }

    private fun setupViewAllButton(view: View) {
        view.findViewById<MaterialButton>(R.id.btnViewAllTransactions)?.setOnClickListener {
            startActivity(Intent(requireContext(), AllTransactionsActivity::class.java))
            requireActivity().overridePendingTransition(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )
        }
    }

    private fun showTransactionSuccessSheet(tx: Transaction) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottom_sheet_transaction_success)
        dialog.setCancelable(true)

        dialog.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.CENTER)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.4f)
        }

        val tvTitle = dialog.findViewById<TextView>(R.id.tvTitle)
        val tvAmount = dialog.findViewById<TextView>(R.id.tvAmount)
        val tvTypeCategory = dialog.findViewById<TextView>(R.id.tvTypeCategory)
        val tvDate = dialog.findViewById<TextView>(R.id.tvDate)
        val tvNote = dialog.findViewById<TextView>(R.id.tvNote)
        val btnAddAnother = dialog.findViewById<MaterialButton>(R.id.btnAddAnother)
        val btnViewAll = dialog.findViewById<MaterialButton>(R.id.btnViewAll)
        val btnClose = dialog.findViewById<MaterialButton>(R.id.btnClose)

        val vnFormat = NumberFormat.getInstance(Locale("vi", "VN"))
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN"))
        val typeLabel = if (tx.type == "income") "Thu nhập" else "Chi tiêu"

        tvTitle.text = "Đã lưu $typeLabel"
        tvAmount.text = "${vnFormat.format(tx.amount)} đ"
        tvTypeCategory.text = "$typeLabel • ${tx.categoryName}"
        tvDate.text = "Lúc ${sdf.format(tx.date.toDate())}"

        if (tx.title.isNullOrBlank()) {
            tvNote.visibility = View.GONE
        } else {
            tvNote.text = "Ghi chú: ${tx.title}"
            tvNote.visibility = View.VISIBLE
        }

        btnAddAnother.setOnClickListener {
            dialog.dismiss()
            openAddTransactionFlow(tx.type)
        }

        btnViewAll.setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(requireContext(), AllTransactionsActivity::class.java))
            requireActivity().overridePendingTransition(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    // ======================= GOAL WARNING =======================

    private fun formatVnd(value: Double): String =
        "${NumberFormat.getInstance(Locale("vi", "VN")).format(value)} đ"

    private fun bestNameFromAuth(displayName: String?, email: String?): String {
        if (!displayName.isNullOrBlank()) return displayName
        val name = email?.substringBefore("@") ?: "Người dùng"
        return name.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }
    }

    private fun showToast(msg: String) {
        requireActivity().runOnUiThread {
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkGoalExceeded(onResult: (() -> Unit)? = null) {
        val prefs =
            requireContext().getSharedPreferences("FinlyPrefs", Context.MODE_PRIVATE)

        val monthlyGoal = prefs.getLong("monthlyGoal", 0L)
        val threshold = prefs.getInt("notificationThreshold", 80)
        val notifEnabled = prefs.getBoolean("notificationEnabled", true)

        if (!notifEnabled || monthlyGoal == 0L) {
            onResult?.invoke()
            return
        }

        val limit = monthlyGoal * threshold / 100

        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .collection("transactions")
            .whereEqualTo("type", "spending")
            .get()
            .addOnSuccessListener { docs ->
                var total = 0.0
                docs.forEach { total += it.getDouble("amount") ?: 0.0 }

                if (total >= limit) {
                    showGoalWarningDialog(total, monthlyGoal, threshold)
                }

                onResult?.invoke()
            }
    }

    private fun showGoalWarningDialog(current: Double, goal: Long, threshold: Int) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_goal_warning)

        dialog.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.CENTER)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.35f)
        }

        val percent = ((current / goal) * 100).toInt()
        val tv = dialog.findViewById<TextView>(R.id.tvWarningMessage)
        val btn = dialog.findViewById<Button>(R.id.btnOk)

        tv.text =
            "⚠️ Bạn đã đạt $percent% mục tiêu chi tiêu tháng!\nNgưỡng cảnh báo: $threshold%"

        btn.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }
}