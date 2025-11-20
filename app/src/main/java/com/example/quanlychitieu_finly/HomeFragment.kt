package com.example.quanlychitieu_finly

import Category.Category
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.Window
import android.view.WindowManager


class HomeFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var rvTransactions: RecyclerView
    private lateinit var adapter: TransactionAdapter
    private val transactionList = mutableListOf<Transaction>()

    private var tvIncome: TextView? = null
    private var tvExpense: TextView? = null
    private var tvBalance: TextView? = null
    private var tvCount: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Nếu chưa đăng nhập -> LoginActivity
        if (!ensureLoggedIn()) return view

        bindUserHeader(view)

        rvTransactions = view.findViewById(R.id.rvTransactions)
        adapter = TransactionAdapter(transactionList)
        rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        rvTransactions.adapter = adapter

        val btnAddIncome: MaterialCardView = view.findViewById(R.id.add_income_layout)
        val btnAddExpense: MaterialCardView = view.findViewById(R.id.add_expense_layout)
        btnAddIncome.setOnClickListener { openAddTransactionFlow("income") }
        btnAddExpense.setOnClickListener { openAddTransactionFlow("spending") }
        val btnReports: MaterialCardView = view.findViewById(R.id.btnReports)
        btnReports.setOnClickListener {
            (activity as? MainActivity)?.switchToAnalyticsTab()
        }


        tvIncome = view.findViewById(R.id.tvIncome)
        tvExpense = view.findViewById(R.id.tvExpense)
        tvBalance = view.findViewById(R.id.tvBalance)
        tvCount = view.findViewById(R.id.tvTransactionCount)

        val tvDate = view.findViewById<TextView>(R.id.tvDate)
        val df = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("vi", "VN"))
        tvDate.text = df.format(Date())

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

    private fun ensureLoggedIn(): Boolean {
        val user = auth.currentUser
        return if (user == null) {
            redirectToLogin()
            false
        } else true
    }

    private fun redirectToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        requireActivity().finish()
    }

    private fun bindUserHeader(root: View) {
        val tvUserName = root.findViewById<TextView>(R.id.tvUserName)
        val imgAvatar = root.findViewById<ImageView>(R.id.imgAvatar)
        val user = auth.currentUser ?: return

        tvUserName.text = bestNameFromAuth(user.displayName, user.email)
        user.photoUrl?.let {
            Glide.with(requireContext()).load(it).into(imgAvatar)
        }

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                doc.getString("fullName")?.let { tvUserName.text = it }
                doc.getString("avatarUrl")?.let {
                    Glide.with(requireContext()).load(it).into(imgAvatar)
                }
            }
    }

    // ========== Mở Bottom Sheet Thêm giao dịch ==========
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
                    Toast.makeText(requireContext(), "Chưa có danh mục $type", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                showAddTransactionSheet(type, categories)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Lỗi tải danh mục: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAddTransactionSheet(type: String, categories: List<Category>) {
        val dialog = BottomSheetDialog(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog)
        val v = layoutInflater.inflate(R.layout.bottom_sheet_add_transaction, null)
        dialog.setContentView(v)

        val tvTitle = v.findViewById<TextView>(R.id.tvSheetTitle)
        val edtTitle = v.findViewById<TextInputEditText>(R.id.edtTitle)
        val edtCategory = v.findViewById<TextInputEditText>(R.id.edtCategory)
        val edtAmount = v.findViewById<TextInputEditText>(R.id.edtAmount)
        val btnSave = v.findViewById<MaterialButton>(R.id.btnSave)
        val btnCancel = v.findViewById<MaterialButton>(R.id.btnCancel)

        tvTitle.text = if (type == "income") "Thêm thu nhập" else "Thêm chi tiêu"
        var chosen: Category? = categories.firstOrNull()
        edtCategory.setText(chosen?.name ?: "")

        // ===== Chọn danh mục bằng sheet =====
        edtCategory.setOnClickListener {
            val catList = categories.sortedBy { it.name.lowercase(Locale.ROOT) }
            val pickDialog = BottomSheetDialog(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog)
            val pickView = layoutInflater.inflate(R.layout.bottom_sheet_category_picker, null)
            pickDialog.setContentView(pickView)
            val rv = pickView.findViewById<RecyclerView>(R.id.rvCategories)
            rv.layoutManager = LinearLayoutManager(requireContext())
            rv.adapter = CategoryPickerAdapter(catList.map { CatUI(it.id, it.name, it.iconUrl ?: "") }) {
                chosen = categories.find { c -> c.id == it.id }
                edtCategory.setText(it.name)
                pickDialog.dismiss()
            }
            pickView.findViewById<MaterialButton>(R.id.btnCancel)?.setOnClickListener { pickDialog.dismiss() }
            pickDialog.show()
        }

        // ===== Định dạng tiền =====
        val vnFormat = NumberFormat.getInstance(Locale("vi", "VN"))
        edtAmount.keyListener = DigitsKeyListener.getInstance("0123456789.,")
        edtAmount.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(18))
        edtAmount.addTextChangedListener(object : TextWatcher {
            var isFormatting = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                val raw = s?.toString() ?: return
                val digits = raw.replace(".", "").replace(",", "").replace("[^0-9]".toRegex(), "")
                if (digits.isEmpty()) { isFormatting = true; edtAmount.setText(""); isFormatting = false; return }
                val parsed = digits.toDoubleOrNull() ?: return
                val formatted = vnFormat.format(parsed)
                if (formatted != raw) {
                    isFormatting = true; edtAmount.setText(formatted)
                    edtAmount.setSelection(formatted.length); isFormatting = false
                }
            }
        })

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val title = edtTitle.text?.toString()?.trim().orEmpty()
            val clean = edtAmount.text?.toString()
                ?.replace(".", "")
                ?.replace(",", "")
                ?.trim()
                .orEmpty()
            val amount = clean.toDoubleOrNull() ?: 0.0

            if (title.isEmpty() || chosen == null || amount <= 0) {
                Toast.makeText(requireContext(), "Vui lòng nhập đủ thông tin hợp lệ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // GỌI HÀM MỚI
            saveTransaction(type, title, chosen!!, amount) { savedTx ->
                showTransactionSuccessSheet(savedTx)
            }

            dialog.dismiss()
        }


        dialog.show()
    }
    private fun showCenteredTransactionDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottom_sheet_transaction_success)

        dialog.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.CENTER)   // <-- Quan trọng: popup nằm giữa màn
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.4f) // độ tối nền
        }

        dialog.show()
    }

    // ========== Lưu giao dịch ==========
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
            onSuccess(tx)
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }


    // ========== Load dữ liệu ==========
    private fun loadRecentTransactions() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .collection("transactions")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(3)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                transactionList.clear()
                snapshot?.forEach { doc -> transactionList.add(doc.toObject(Transaction::class.java)) }
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
            requireActivity().overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
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
            setGravity(Gravity.CENTER)                    // ⭐ Nằm giữa màn hình
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.4f)                            // Nền tối mờ phía sau
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

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }


    private fun formatVnd(value: Double): String =
        "${NumberFormat.getInstance(Locale("vi", "VN")).format(value)} đ"

    private fun bestNameFromAuth(displayName: String?, email: String?): String {
        if (!displayName.isNullOrBlank()) return displayName
        val name = email?.substringBefore("@") ?: "Người dùng"
        return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }
}
