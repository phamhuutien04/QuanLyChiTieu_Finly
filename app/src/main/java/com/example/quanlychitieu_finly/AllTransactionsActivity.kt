package com.example.quanlychitieu_finly

import Category.Category
import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class AllTransactionsActivity : AppCompatActivity() {

    private lateinit var tgTypeFilter: MaterialButtonToggleGroup
    private lateinit var btnFilterIncome: MaterialButton
    private lateinit var btnFilterExpense: MaterialButton
    private lateinit var edtSearchTransaction: TextInputEditText
    private lateinit var btnSelectCategoryFilter: MaterialButton

    private var currentTypeFilter: String = "income"
    private var currentCategoryFilter: Category? = null
    private var allCategories: List<Category> = emptyList()

    private var txListener: ListenerRegistration? = null

    private lateinit var rvAllTransactions: RecyclerView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpense: TextView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Không cần list 'transactions' để hiển thị nữa, Adapter tự lo
    private val fullTransactionList = mutableListOf<Transaction>()
    private val docIds = mutableListOf<String>()

    private lateinit var adapter: TransactionAdapter

    private val vnLocale = Locale("vi", "VN")
    private val vnNumberFormat: NumberFormat by lazy { NumberFormat.getInstance(vnLocale) }

    companion object {
        private const val TAG = "AllTransactionsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_transactions)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Ràng buộc Views
        rvAllTransactions = findViewById(R.id.rvAllTransactions)
        tvTotalIncome = findViewById(R.id.tvTotalIncome)
        tvTotalExpense = findViewById(R.id.tvTotalExpense)
        tgTypeFilter = findViewById(R.id.tgTypeFilter)
        btnFilterIncome = findViewById(R.id.btnFilterIncome)
        btnFilterExpense = findViewById(R.id.btnFilterExpense)
        edtSearchTransaction = findViewById(R.id.edtSearchTransaction)
        btnSelectCategoryFilter = findViewById(R.id.btnSelectCategoryFilter)

        // Xóa tham số 'enableGrouping = true' vì Adapter mới mặc định đã gom nhóm
        adapter = TransactionAdapter { item ->
            showEditDeleteSheet(item)
        }

        rvAllTransactions.layoutManager = LinearLayoutManager(this)
        rvAllTransactions.adapter = adapter
        (rvAllTransactions.itemAnimator as? androidx.recyclerview.widget.SimpleItemAnimator)
            ?.supportsChangeAnimations = false

        currentTypeFilter = "income"
        tgTypeFilter.check(btnFilterIncome.id)
        updateButtonColors()
        loadTotalAmounts()
        loadAllCategories()
        attachTxListener()

        tgTypeFilter.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val newType = if (checkedId == btnFilterIncome.id) "income" else "spending"
            if (newType != currentTypeFilter) {
                currentTypeFilter = newType
                currentCategoryFilter = null // Reset Category khi đổi Type
                btnSelectCategoryFilter.text = "Danh mục"
                updateButtonColors()
                filterAndDisplayTransactions() // Chỉ lọc lại
            }
        }

        setupSearchAndFilter()
    }

    // ========================= NEW: SEARCH AND FILTER SETUP =================
    private fun setupSearchAndFilter() {
        // 1. Tìm kiếm theo tiêu đề giao dịch
        edtSearchTransaction.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterAndDisplayTransactions()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 2. Lọc theo danh mục
        btnSelectCategoryFilter.setOnClickListener {
            showCategoryFilterSheet()
        }
    }

    private fun loadAllCategories() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .collection("categories")
            .orderBy("type")
            .orderBy("name")
            .get()
            .addOnSuccessListener { snap ->
                allCategories = snap.documents.mapNotNull { doc ->
                    doc.toObject(Category::class.java)?.apply { id = doc.id }
                }
            }
    }

    // --- SỬA 1: Truyền ID danh mục đang lọc vào Adapter ---
    private fun showCategoryFilterSheet() {
        val categoriesOfType = allCategories.filter { it.type == currentTypeFilter }

        if (categoriesOfType.isEmpty()) {
            Toast.makeText(this, "Chưa có danh mục $currentTypeFilter", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = BottomSheetDialog(this)
        val pv = layoutInflater.inflate(R.layout.bottom_sheet_category_picker, null)
        dialog.setContentView(pv)

        val rv = pv.findViewById<RecyclerView>(R.id.rvCategories)
        rv.layoutManager = LinearLayoutManager(this)

        val allCat = CatUI("all", "Tất cả Danh mục", "")
        val listUI = mutableListOf(allCat)
        listUI.addAll(categoriesOfType.map { CatUI(it.id, it.name, it.iconUrl ?: "") })

        // Lấy ID đang chọn (nếu null thì mặc định là 'all')
        val currentId = currentCategoryFilter?.id ?: "all"

        rv.adapter = CategoryPickerAdapter(listUI, currentId) { selectedCatUI ->
            currentCategoryFilter = if (selectedCatUI.id == "all") null else allCategories.find { c -> c.id == selectedCatUI.id }
            btnSelectCategoryFilter.text = currentCategoryFilter?.name ?: "Danh mục"
            filterAndDisplayTransactions()
            dialog.dismiss()
        }

        pv.findViewById<MaterialButton>(R.id.btnCancel)?.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    /**
     * Lọc và hiển thị giao dịch
     */
    private fun filterAndDisplayTransactions() {
        val queryText = edtSearchTransaction.text?.toString().orEmpty().lowercase(vnLocale)

        // Bắt đầu lọc từ danh sách đầy đủ
        val finalFilteredList = fullTransactionList.filter { tx ->
            val matchesType = tx.type.equals(currentTypeFilter, true)
            val matchesCategory = if (currentCategoryFilter == null) true else tx.categoryId == currentCategoryFilter!!.id
            val matchesTitle = tx.title.lowercase(vnLocale).contains(queryText)

            matchesType && matchesCategory && matchesTitle
        }

        // Gọi setData của Adapter mới
        adapter.setData(finalFilteredList)
    }

    // ========================= LOAD TRANSACTIONS ==============================
    private fun attachTxListener() {
        val userId = auth.currentUser?.uid ?: return
        txListener?.remove()

        val query = db.collection("users").document(userId)
            .collection("transactions")
            .orderBy("date", Query.Direction.DESCENDING)

        txListener = query.addSnapshotListener { documents, error ->
            if (error != null) {
                Toast.makeText(this, "Lỗi tải dữ liệu: ${error.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            fullTransactionList.clear()
            docIds.clear()
            documents?.forEach { doc ->
                val t = doc.toObject(Transaction::class.java)
                fullTransactionList.add(t)
                docIds.add(doc.id)
            }

            filterAndDisplayTransactions()
            loadTotalAmounts()
        }
    }

    private fun loadTotalAmounts() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .collection("transactions")
            .get()
            .addOnSuccessListener { allDocs ->
                var totalIncome = 0.0
                var totalExpense = 0.0
                for (doc in allDocs) {
                    val t = doc.toObject(Transaction::class.java)
                    if (t.type.equals("income", true)) totalIncome += t.amount
                    else totalExpense += t.amount
                }
                tvTotalIncome.text = formatVnd(totalIncome)
                tvTotalExpense.text = formatVnd(totalExpense)
            }
    }

    // ========================= EDIT / DELETE LOGIC ============================

    // --- SỬA 2: Thêm tham số currentId vào openCategorySheet và truyền cho Adapter ---
    private fun openCategorySheet(
        currentType: String,
        currentId: String? = null,
        onPicked: (CatUI) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .collection("categories")
            .whereEqualTo("type", currentType)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.mapNotNull { d ->
                    val id = d.id
                    val name = d.getString("name") ?: return@mapNotNull null
                    val icon = d.getString("iconUrl") ?: ""
                    CatUI(id, name, icon)
                }.sortedBy { it.name.lowercase(Locale.ROOT) }

                if (list.isEmpty()) {
                    Toast.makeText(this, "Chưa có danh mục $currentType", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val dialog = BottomSheetDialog(
                    this,
                    com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog
                )
                val v = layoutInflater.inflate(R.layout.bottom_sheet_category_picker, null)
                dialog.setContentView(v)

                val rv = v.findViewById<RecyclerView>(R.id.rvCategories)
                rv.layoutManager = LinearLayoutManager(this)

                // Truyền currentId vào Adapter
                rv.adapter = CategoryPickerAdapter(list, currentId) {
                    onPicked(it)
                    dialog.dismiss()
                }
                v.findViewById<MaterialButton>(R.id.btnCancel)
                    .setOnClickListener { dialog.dismiss() }
                dialog.show()
            }
    }

    private fun showEditDeleteSheet(item: Transaction) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_transaction_actions, null)
        dialog.setContentView(view)

        val edtTitle = view.findViewById<TextInputEditText>(R.id.edtTitle)
        val edtAmount = view.findViewById<TextInputEditText>(R.id.edtAmount)
        val edtCategory = view.findViewById<TextInputEditText>(R.id.edtCategory)
        val edtDate = view.findViewById<TextInputEditText>(R.id.edtDate) // Ánh xạ View Ngày

        val toggle = view.findViewById<MaterialButtonToggleGroup>(R.id.toggleType)
        val btnIncome = view.findViewById<MaterialButton>(R.id.btnIncome)
        val btnExpense = view.findViewById<MaterialButton>(R.id.btnExpense)
        val btnDelete = view.findViewById<MaterialButton>(R.id.btnDelete)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)

        var chosenCat: CatUI? = null

        // --- XỬ LÝ NGÀY THÁNG ---
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        var selectedDate: Date = item.date.toDate()
        edtDate.setText(sdf.format(selectedDate))

        edtDate.setOnClickListener {
            val cal = Calendar.getInstance()
            cal.time = selectedDate

            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    cal.set(year, month, dayOfMonth)
                    selectedDate = cal.time
                    edtDate.setText(sdf.format(selectedDate))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // --- SỬA 3: Helper pickCategory xác định ID danh mục hiện tại ---
        fun pickCategory(type: String) {
            val currentSelectionId = chosenCat?.id ?: item.categoryId
            openCategorySheet(type, currentSelectionId) { picked ->
                chosenCat = picked
                edtCategory.setText(picked.name)
            }
        }

        // Prefill data
        edtTitle.setText(item.title)
        edtAmount.setText(formatPlainNumber(item.amount))
        edtAmount.setSelection(edtAmount.text?.length ?: 0)
        edtAmount.keyListener = DigitsKeyListener.getInstance("0123456789.,")
        edtAmount.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(18))
        attachVndTextWatcher(edtAmount)

        if (item.type.equals("income", true)) toggle.check(btnIncome.id) else toggle.check(btnExpense.id)
        edtCategory.setText(item.categoryName)

        edtCategory.setOnClickListener {
            val typeNow = if (toggle.checkedButtonId == btnIncome.id) "income" else "spending"
            pickCategory(typeNow)
        }

        toggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val newType = if (checkedId == btnIncome.id) "income" else "spending"
                // Reset chọn danh mục nếu đổi loại, vì ID cũ có thể không tồn tại bên loại mới
                chosenCat = null
                edtCategory.setText("")
                pickCategory(newType)
            }
        }

        // Xử lý XÓA: Truyền item
        btnDelete.setOnClickListener {
            confirmDelete(item)
            dialog.dismiss()
        }

        // Xử lý LƯU: Truyền item cũ
        btnSave.setOnClickListener {
            val newTitle = edtTitle.text?.toString()?.trim().orEmpty()
            val newAmount = parseVndToDouble(edtAmount.text?.toString().orEmpty())
            val newType = if (toggle.checkedButtonId == btnIncome.id) "income" else "spending"

            if (newTitle.isBlank() || newAmount == null || newAmount <= 0.0) {
                Toast.makeText(this, "Nhập tiêu đề và số tiền hợp lệ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (chosenCat == null && edtCategory.text.isNullOrBlank()) {
                Toast.makeText(this, "Hãy chọn danh mục", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val catToSave = chosenCat ?: CatUI(item.categoryId, item.categoryName, item.categoryIconUrl)

            // Gọi hàm Update, truyền thêm Timestamp(selectedDate)
            updateTransaction(item, newTitle, newAmount, newType, catToSave, Timestamp(selectedDate))
            dialog.dismiss()
        }

        dialog.show()
    }

    // --- Update Logic: Nhận thêm tham số newDate ---
    private fun updateTransaction(
        oldItem: Transaction,
        newTitle: String,
        newAmount: Double,
        newType: String,
        newCat: CatUI,
        newDate: Timestamp
    ) {
        // Tìm index trong list gốc để lấy ID tương ứng
        val actualIndex = fullTransactionList.indexOfFirst {
            it.date == oldItem.date && it.title == oldItem.title && it.amount == oldItem.amount
        }

        if (actualIndex == -1 || actualIndex >= docIds.size) {
            Toast.makeText(this, "Lỗi: Không tìm thấy giao dịch gốc", Toast.LENGTH_LONG).show()
            return
        }

        val docId = docIds[actualIndex]
        val userId = auth.currentUser?.uid ?: return

        val updates = mapOf(
            "title" to newTitle,
            "amount" to newAmount,
            "type" to newType,
            "categoryId" to newCat.id,
            "categoryName" to newCat.name,
            "categoryIconUrl" to newCat.iconUrl,
            "date" to newDate // Lưu ngày mới
        )

        db.collection("users").document(userId)
            .collection("transactions").document(docId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Đã lưu thay đổi", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmDelete(item: Transaction) {
        AlertDialog.Builder(this)
            .setTitle("Xóa giao dịch")
            .setMessage("Bạn có chắc muốn xóa giao dịch: ${item.title}?")
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Xóa") { _, _ -> deleteTransaction(item) }
            .show()
    }

    // --- Delete Logic: Tìm ID dựa trên Object Transaction ---
    private fun deleteTransaction(item: Transaction) {
        val actualIndex = fullTransactionList.indexOfFirst {
            it.date == item.date && it.title == item.title && it.amount == item.amount
        }

        if (actualIndex == -1 || actualIndex >= docIds.size) {
            Toast.makeText(this, "Lỗi: Không tìm thấy giao dịch gốc để xóa", Toast.LENGTH_LONG).show()
            return
        }

        val docId = docIds[actualIndex]
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("transactions").document(docId)
            .delete()
            .addOnSuccessListener { Toast.makeText(this, "Đã xóa", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Toast.makeText(this, "Lỗi xóa: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    // ========================= UTILS ============================

    private fun formatPlainNumber(value: Double): String = vnNumberFormat.format(value)
    private fun formatVnd(value: Double): String = "${vnNumberFormat.format(value)} đ"
    private fun parseVndToDouble(input: String): Double? {
        val cleaned = input.replace(".", "").replace(",", "").trim()
        return cleaned.toDoubleOrNull()
    }

    private fun attachVndTextWatcher(editText: TextInputEditText) {
        var isFormatting = false
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                val raw = s?.toString() ?: return
                val digitsOnly = raw.replace(".", "").replace(",", "").replace("[^0-9]".toRegex(), "")
                if (digitsOnly.isEmpty()) {
                    isFormatting = true; editText.setText(""); isFormatting = false; return
                }
                val parsed = digitsOnly.toDoubleOrNull() ?: return
                val formatted = vnNumberFormat.format(parsed)
                if (formatted != raw) {
                    isFormatting = true
                    editText.setText(formatted)
                    editText.setSelection(formatted.length)
                    isFormatting = false
                }
            }
        })
    }

    private fun updateButtonColors() {
        val income = ContextCompat.getColor(this, R.color.income_color)
        val expense = ContextCompat.getColor(this, R.color.expense_color)
        val textWhite = ContextCompat.getColor(this, R.color.text_white)
        val textGray = ContextCompat.getColor(this, R.color.text_gray)
        val strokeGray = ContextCompat.getColor(this, R.color.stroke_gray)

        fun MaterialButton.setSelectedStyle(bgColor: Int) {
            backgroundTintList = ColorStateList.valueOf(bgColor)
            setTextColor(textWhite)
            strokeColor = ColorStateList.valueOf(bgColor)
        }
        fun MaterialButton.setUnselectedStyle() {
            backgroundTintList = ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
            setTextColor(textGray)
            strokeColor = ColorStateList.valueOf(strokeGray)
        }

        if (currentTypeFilter == "income") {
            btnFilterIncome.setSelectedStyle(income)
            btnFilterExpense.setUnselectedStyle()
        } else {
            btnFilterIncome.setUnselectedStyle()
            btnFilterExpense.setSelectedStyle(expense)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        txListener?.remove()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}