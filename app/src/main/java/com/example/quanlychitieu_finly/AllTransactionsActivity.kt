package com.example.quanlychitieu_finly

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.util.Locale
import com.google.firebase.firestore.ListenerRegistration
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.core.content.ContextCompat

class AllTransactionsActivity : AppCompatActivity() {
    private lateinit var tgTypeFilter: com.google.android.material.button.MaterialButtonToggleGroup
    private lateinit var btnFilterIncome: MaterialButton
    private lateinit var btnFilterExpense: MaterialButton

    private var currentFilter: String = "income" // "income" | "spending"
    private var txListener: ListenerRegistration? = null
    private lateinit var rvAllTransactions: RecyclerView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpense: TextView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val transactions = mutableListOf<Transaction>()
    private val docIds = mutableListOf<String>()
    private lateinit var adapter: TransactionAdapter

    // ---- Helpers VNĐ ----
    private val vnLocale = Locale("vi", "VN")
    private val vnNumberFormat: NumberFormat by lazy { NumberFormat.getInstance(vnLocale) }

    companion object {
        private const val TAG = "AllTransactionsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_transactions)

        // Toolbar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Quay lại"

        // Views
        rvAllTransactions = findViewById(R.id.rvAllTransactions)
        tvTotalIncome     = findViewById(R.id.tvTotalIncome)
        tvTotalExpense    = findViewById(R.id.tvTotalExpense)

        // Toggle filter
        tgTypeFilter     = findViewById(R.id.tgTypeFilter)
        btnFilterIncome  = findViewById(R.id.btnFilterIncome)
        btnFilterExpense = findViewById(R.id.btnFilterExpense)

        // RecyclerView
        adapter = TransactionAdapter(transactions) { position, item ->
            showEditDeleteSheet(position, item)
        }
        rvAllTransactions.layoutManager = LinearLayoutManager(this)
        rvAllTransactions.adapter = adapter
        (rvAllTransactions.itemAnimator as? androidx.recyclerview.widget.SimpleItemAnimator)
            ?.supportsChangeAnimations = false

        // Mặc định: Thu nhập
        currentFilter = "income"
        tgTypeFilter.check(btnFilterIncome.id)

        // Set màu ban đầu
        updateButtonColors()

        // Load tổng thu chi ngay từ đầu
        loadTotalAmounts()

        // Gắn listener để load danh sách theo filter
        attachTxListener()

        tgTypeFilter.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            val newFilter = if (checkedId == btnFilterIncome.id) "income" else "spending"

            // Chỉ attach lại nếu filter thay đổi
            if (newFilter != currentFilter) {
                currentFilter = newFilter
                Log.d(TAG, "Filter changed to: $currentFilter")
                updateButtonColors()
                attachTxListener()
            }
        }
    }

    private fun attachTxListener() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "User not logged in")
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show()
            return
        }

        // Hủy listener cũ trước khi gắn mới
        txListener?.remove()
        txListener = null

        Log.d(TAG, "Attaching listener for filter: $currentFilter")
        Log.d(TAG, "UserId: $userId")

        // DEBUG: Trước tiên, load ALL transactions để xem có dữ liệu không
        db.collection("users").document(userId)
            .collection("transactions")
            .get()
            .addOnSuccessListener { allDocs ->
                Log.d(TAG, "=== DEBUG: Total transactions in DB: ${allDocs.size()} ===")
                allDocs.forEachIndexed { index, doc ->
                    val type = doc.getString("type")
                    val amount = doc.getDouble("amount")
                    val title = doc.getString("title")
                    Log.d(TAG, "Transaction $index: type='$type', amount=$amount, title='$title'")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load all transactions: ${e.message}", e)
            }

        val query = db.collection("users").document(userId)
            .collection("transactions")
            .whereEqualTo("type", currentFilter)
            .orderBy("date", Query.Direction.DESCENDING)

        txListener = query.addSnapshotListener { documents, error ->
            if (error != null) {
                Log.e(TAG, "Error loading transactions: ${error.message}", error)
                Toast.makeText(this, "Lỗi tải dữ liệu: ${error.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            if (documents == null) {
                Log.w(TAG, "No documents found")
                return@addSnapshotListener
            }

            Log.d(TAG, "Loaded ${documents.size()} transactions for filter: $currentFilter")

            val newList = mutableListOf<Transaction>()
            docIds.clear()

            for (doc in documents) {
                try {
                    val t = doc.toObject(Transaction::class.java)
                    newList.add(t)
                    docIds.add(doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing transaction: ${e.message}", e)
                }
            }

            adapter.replaceAll(newList)

            // Reload tổng sau khi có thay đổi
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
                    try {
                        val t = doc.toObject(Transaction::class.java)
                        if (t.type.equals("income", ignoreCase = true)) {
                            totalIncome += t.amount
                        } else if (t.type.equals("spending", ignoreCase = true) || t.type.equals("expense", ignoreCase = true)) {
                            totalExpense += t.amount
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing transaction for totals: ${e.message}", e)
                    }
                }

                tvTotalIncome.text = formatVnd(totalIncome)
                tvTotalExpense.text = formatVnd(totalExpense)

                Log.d(TAG, "Total Income: $totalIncome, Total Expense: $totalExpense")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading totals: ${e.message}", e)
            }
    }

    private fun showEditDeleteSheet(position: Int, item: Transaction) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_transaction_actions, null, false)
        dialog.setContentView(view)

        val edtTitle  = view.findViewById<TextInputEditText>(R.id.edtTitle)
        val edtAmount = view.findViewById<TextInputEditText>(R.id.edtAmount)
        val toggle    = view.findViewById<MaterialButtonToggleGroup>(R.id.toggleType)
        val btnIncome = view.findViewById<MaterialButton>(R.id.btnIncome)
        val btnExpense= view.findViewById<MaterialButton>(R.id.btnExpense)
        val btnDelete = view.findViewById<MaterialButton>(R.id.btnDelete)
        val btnSave   = view.findViewById<MaterialButton>(R.id.btnSave)

        // Prefill
        edtTitle.setText(item.title)
        edtAmount.setText(formatPlainNumber(item.amount))
        edtAmount.setSelection(edtAmount.text?.length ?: 0)

        // Chỉ cho nhập số & . ,  (chặn "E", "-")
        edtAmount.keyListener = DigitsKeyListener.getInstance("0123456789.,")
        edtAmount.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(18))
        attachVndTextWatcher(edtAmount)

        if (item.type.equals("income", ignoreCase = true)) {
            toggle.check(btnIncome.id)
        } else {
            toggle.check(btnExpense.id)
        }

        // XÓA
        btnDelete.setOnClickListener {
            confirmDelete(position)
            dialog.dismiss()
        }

        // LƯU
        btnSave.setOnClickListener {
            val newTitle  = edtTitle.text?.toString()?.trim().orEmpty()
            val newAmount = parseVndToDouble(edtAmount.text?.toString().orEmpty())
            val newType   = if (toggle.checkedButtonId == btnIncome.id) "income" else "spending"

            if (newTitle.isBlank() || newAmount == null || newAmount <= 0.0) {
                Toast.makeText(this, "Nhập tiêu đề và số tiền hợp lệ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            updateTransaction(position, item, newTitle, newAmount, newType)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateTransaction(
        position: Int,
        oldItem: Transaction,
        newTitle: String,
        newAmount: Double,
        newType: String
    ) {
        if (position !in docIds.indices) return
        val userId = auth.currentUser?.uid ?: return
        val docId = docIds[position]
        val docRef = db.collection("users").document(userId)
            .collection("transactions").document(docId)

        val adjustCategoryTotals = false // bật true nếu có categoryId & muốn cập nhật

        val oldAmountSigned = if (oldItem.type.equals("income", ignoreCase = true)) oldItem.amount else -oldItem.amount
        val newAmountSigned = if (newType.equals("income", ignoreCase = true)) newAmount else -newAmount
        val delta = newAmountSigned - oldAmountSigned

        val updates = mapOf(
            "title" to newTitle,
            "amount" to newAmount,
            "type" to newType,
            "date" to (oldItem.date ?: Timestamp.now())
        )

        docRef.update(updates)
            .addOnSuccessListener {
                if (adjustCategoryTotals && !oldItem.categoryId.isNullOrBlank()) {
                    val catRef = db.collection("users").document(userId)
                        .collection("categories").document(oldItem.categoryId!!)
                    catRef.update("totalAmount", FieldValue.increment(delta.toLong()))
                }
                Toast.makeText(this, "Đã lưu thay đổi", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Transaction updated successfully")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error updating transaction: ${e.message}", e)
            }
    }

    private fun confirmDelete(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Xóa giao dịch")
            .setMessage("Bạn có chắc muốn xóa giao dịch này?")
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Xóa") { _, _ -> deleteTransaction(position) }
            .show()
    }

    private fun deleteTransaction(position: Int) {
        if (position !in docIds.indices) return
        val userId = auth.currentUser?.uid ?: return
        val docId = docIds[position]

        db.collection("users").document(userId)
            .collection("transactions").document(docId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Đã xóa", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Transaction deleted successfully")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error deleting transaction: ${e.message}", e)
            }
    }

    // ---------- VND utils ----------
    /** Hiển thị: 1500000.0 -> "1.500.000" */
    private fun formatPlainNumber(value: Double): String = vnNumberFormat.format(value)

    /** Hiển thị với " đ": 1500000.0 -> "1.500.000 đ" */
    private fun formatVnd(value: Double): String = "${vnNumberFormat.format(value)} đ"

    /** Parse "1.500.000" hoặc "1,500,000" -> 1500000.0 */
    private fun parseVndToDouble(input: String): Double? {
        val cleaned = input.replace(".", "").replace(",", "").trim()
        if (cleaned.isBlank()) return null
        return cleaned.toDoubleOrNull()
    }

    /** Auto-format theo VND khi người dùng gõ */
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
                    isFormatting = true
                    editText.setText("")
                    isFormatting = false
                    return
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

    /** Cập nhật màu của các button toggle */
    private fun updateButtonColors() {
        val income = ContextCompat.getColor(this, R.color.income_color)
        val expense = ContextCompat.getColor(this, R.color.expense_color)
        val textWhite = ContextCompat.getColor(this, R.color.text_white)
        val textGray  = ContextCompat.getColor(this, R.color.text_gray)
        val strokeGray = ContextCompat.getColor(this, R.color.stroke_gray)
        val unselectedBg = ContextCompat.getColor(this, R.color.toggle_gray_bg) // #F5F5F5 nếu bạn đã khai báo

        fun MaterialButton.setSelectedStyle(bgColor: Int) {
            backgroundTintList = ColorStateList.valueOf(bgColor)
            setTextColor(textWhite)
            setStrokeColor(ColorStateList.valueOf(bgColor))
            // (tuỳ chọn) bảo đảm có viền:
            // strokeWidth = resources.getDimensionPixelSize(R.dimen.toggle_stroke_width)
        }

        fun MaterialButton.setUnselectedStyle() {
            backgroundTintList = ColorStateList.valueOf(unselectedBg) // hoặc Color.TRANSPARENT
            setTextColor(textGray)
            setStrokeColor(ColorStateList.valueOf(strokeGray))
        }

        if (currentFilter == "income") {
            btnFilterIncome.setSelectedStyle(income)
            btnFilterExpense.setUnselectedStyle()
        } else {
            btnFilterIncome.setUnselectedStyle()
            btnFilterExpense.setSelectedStyle(expense)
        }
    }
    // -------------------------------

    override fun onDestroy() {
        super.onDestroy()
        txListener?.remove()
        Log.d(TAG, "Listener removed")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}