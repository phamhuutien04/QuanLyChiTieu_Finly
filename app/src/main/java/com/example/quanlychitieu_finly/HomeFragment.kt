//
//package com.example.quanlychitieu_finly
//
//import android.content.Intent
//import android.os.Bundle
//import android.text.Editable
//import android.text.InputFilter
//import android.text.TextWatcher
//import android.text.method.DigitsKeyListener
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.AutoCompleteTextView
//import android.widget.EditText
//import android.widget.ImageView
//import android.widget.TextView
//import android.widget.Toast
//import androidx.fragment.app.Fragment
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.bumptech.glide.Glide
//import com.google.android.material.button.MaterialButton
//import com.google.android.material.card.MaterialCardView
//import com.google.android.material.dialog.MaterialAlertDialogBuilder
//import com.google.firebase.Timestamp
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.firestore.Query
//import java.text.NumberFormat
//import java.text.SimpleDateFormat
//import java.util.Calendar
//import java.util.Date
//import java.util.Locale
//
//class HomeFragment : Fragment() {
//
//    private lateinit var db: FirebaseFirestore
//    private lateinit var auth: FirebaseAuth
//
//    private lateinit var rvTransactions: RecyclerView
//    private lateinit var adapter: TransactionAdapter
//    private val transactionList = mutableListOf<Transaction>()
//
//    private var tvIncome: TextView? = null
//    private var tvExpense: TextView? = null
//    private var tvBalance: TextView? = null
//    private var tvCount: TextView? = null
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
//    ): View {
//        val view = inflater.inflate(R.layout.fragment_home, container, false)
//
//        db = FirebaseFirestore.getInstance()
//        auth = FirebaseAuth.getInstance()
//
//        // Nếu chưa đăng nhập -> chuyển LoginActivity và dừng ở đây
//        if (!ensureLoggedIn()) return view
//
//        // Header: tên, avatar, lời chào
//        bindUserHeader(view)
//
//        // RecyclerView
//        rvTransactions = view.findViewById(R.id.rvTransactions)
//        adapter = TransactionAdapter(transactionList)
//        rvTransactions.layoutManager = LinearLayoutManager(requireContext())
//        rvTransactions.adapter = adapter
//
//        // Buttons (MaterialCardView)
//        val btnAddIncome: MaterialCardView = view.findViewById(R.id.add_income_layout)
//        val btnAddExpense: MaterialCardView = view.findViewById(R.id.add_expense_layout)
//        btnAddIncome.setOnClickListener { openAddTransactionFlow("income") }
//        btnAddExpense.setOnClickListener { openAddTransactionFlow("spending") }
//
//        // Tổng
//        tvIncome = view.findViewById(R.id.tvIncome)
//        tvExpense = view.findViewById(R.id.tvExpense)
//        tvBalance = view.findViewById(R.id.tvBalance)
//        tvCount   = view.findViewById(R.id.tvTransactionCount)
//
//        // Ngày
//        val tvDate = view.findViewById<TextView>(R.id.tvDate)
//        val df = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("vi", "VN"))
//        tvDate.text = df.format(Date())
//
//        // Load chỉ 3 giao dịch gần nhất
//        loadRecentTransactions()
//
//        // Setup button "Xem tất cả"
//        setupViewAllButton(view)
//
//        return view
//    }
//
//    override fun onResume() {
//        super.onResume()
//        // Nếu quay lại fragment mà user đã đăng xuất -> LoginActivity
//        if (ensureLoggedIn()) {
//            // Refresh lại tên & avatar (phòng khi user vừa cập nhật hồ sơ)
//            view?.let { bindUserHeader(it) }
//            // Refresh lại danh sách giao dịch
//            loadRecentTransactions()
//        }
//    }
//
//    /** Trả về true nếu đã đăng nhập; nếu chưa thì chuyển sang LoginActivity và trả false */
//    private fun ensureLoggedIn(): Boolean {
//        val user = auth.currentUser
//        return if (user == null) {
//            redirectToLogin()
//            false
//        } else {
//            true
//        }
//    }
//
//    private fun redirectToLogin() {
//        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
//            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
//        }
//        startActivity(intent)
//        requireActivity().finish()
//    }
//
//    /** Bind tên thật + avatar + lời chào vào header */
//    private fun bindUserHeader(root: View) {
//        val tvUserName = root.findViewById<TextView>(R.id.tvUserName)
//        val imgAvatar  = root.findViewById<ImageView>(R.id.imgAvatar)
//
//        val user = auth.currentUser ?: return
//
//        // Hiển thị tạm thời từ Auth để UI không trễ
//        tvUserName.text = bestNameFromAuth(user.displayName, user.email)
//
//        // Ảnh tạm thời từ Auth (nếu có)
//        user.photoUrl?.let { uri ->
//            Glide.with(requireContext())
//                .load(uri)
//                .placeholder(android.R.drawable.ic_menu_gallery)
//                .error(android.R.drawable.ic_menu_manage)
//                .into(imgAvatar)
//        }
//
//        // Override bằng hồ sơ Firestore nếu có
//        db.collection("users").document(user.uid).get()
//            .addOnSuccessListener { doc ->
//                val fullName  = doc.getString("fullName")
//                val username  = doc.getString("username")
//                val avatarUrl = doc.getString("avatarUrl")
//
//                val chosenName = when {
//                    !fullName.isNullOrBlank() -> fullName
//                    !username.isNullOrBlank() -> username
//                    else -> null
//                }
//                if (!chosenName.isNullOrBlank()) {
//                    tvUserName.text = chosenName
//                }
//
//                if (!avatarUrl.isNullOrBlank()) {
//                    Glide.with(requireContext())
//                        .load(avatarUrl)
//                        .placeholder(android.R.drawable.ic_menu_gallery)
//                        .error(android.R.drawable.ic_menu_manage)
//                        .into(imgAvatar)
//                }
//            }
//            .addOnFailureListener {
//                // Giữ lại tên/ảnh từ Auth nếu Firestore lỗi
//            }
//    }
//
//    /** 1) Lấy categories -> 2) Mở dialog nhập & chọn danh mục -> 3) Lưu transaction */
//    private fun openAddTransactionFlow(type: String) {
//        if (!ensureLoggedIn()) return
//        val userId = auth.currentUser?.uid ?: return
//
//        val wantedType = if (type == "income") "income" else "spending"
//
//        db.collection("users").document(userId)
//            .collection("categories")
//            .whereEqualTo("type", wantedType)
//            .orderBy("name")
//            .get()
//            .addOnSuccessListener { snap ->
//                val categories = snap.documents.mapNotNull { doc ->
//                    doc.toObject(Category::class.java)?.apply { id = doc.id }
//                }
//
//                if (categories.isEmpty()) {
//                    Toast.makeText(requireContext(), "Chưa có danh mục $wantedType. Hãy tạo trước.", Toast.LENGTH_SHORT).show()
//                    return@addOnSuccessListener
//                }
//
//                showAddTransactionDialog(type, categories)
//            }
//            .addOnFailureListener {
//                Toast.makeText(requireContext(), "Lỗi tải danh mục: ${it.message}", Toast.LENGTH_SHORT).show()
//            }
//    }
//
//    /** Hiển thị dialog nhập title/amount + chọn category từ list */
//    private fun showAddTransactionDialog(type: String, categories: List<Category>) {
//        val dialogView = layoutInflater.inflate(R.layout.dialog_add_transaction, null)
//        val edtTitle   = dialogView.findViewById<EditText>(R.id.edtTitle)
//        val edtAmount  = dialogView.findViewById<EditText>(R.id.edtAmount)
//        val actv       = dialogView.findViewById<AutoCompleteTextView>(R.id.actvCategory)
//
//        val catAdapter = CategoryDropdownAdapter(requireContext(), categories)
//        actv.setAdapter(catAdapter)
//
//        var chosen: Category? = categories.firstOrNull()
//        actv.setText(chosen?.name ?: "", false)
//
//        actv.setOnItemClickListener { _, _, position, _ ->
//            chosen = categories[position]
//            actv.setText(chosen!!.name, false)
//        }
//
//        // ====== Định dạng số tiền theo VND ======
//        val vnLocale = Locale("vi", "VN")
//        val vnFormat = NumberFormat.getInstance(vnLocale)
//
//
//        // Tự động thêm dấu chấm khi gõ
//        edtAmount.addTextChangedListener(object : TextWatcher {
//            private var isFormatting = false
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//            override fun afterTextChanged(s: Editable?) {
//                if (isFormatting) return
//                val raw = s?.toString() ?: return
//
//                val digits = raw.replace(".", "").replace(",", "").replace("[^0-9]".toRegex(), "")
//                if (digits.isEmpty()) {
//                    isFormatting = true
//                    edtAmount.setText("")
//                    isFormatting = false
//                    return
//                }
//
//                val parsed = digits.toDoubleOrNull() ?: return
//                val formatted = vnFormat.format(parsed)
//                if (formatted != raw) {
//                    isFormatting = true
//                    edtAmount.setText(formatted)
//                    edtAmount.setSelection(formatted.length)
//                    isFormatting = false
//                }
//            }
//        })
//        // =======================================
//
//        MaterialAlertDialogBuilder(requireContext())
//            .setTitle(if (type == "income") "Thêm thu nhập" else "Thêm chi tiêu")
//            .setView(dialogView)
//            .setPositiveButton("Lưu") { _, _ ->
//                val title = edtTitle.text.toString().trim()
//                val rawInput = edtAmount.text.toString().trim()
//                val clean = rawInput.replace(".", "").replace(",", "")
//                val amount = clean.toDoubleOrNull() ?: 0.0
//
//                if (chosen == null) {
//                    val typed = actv.text?.toString()?.trim().orEmpty()
//                    chosen = categories.find { it.name == typed }
//                }
//
//                if (title.isEmpty() || chosen == null || amount <= 0) {
//                    Toast.makeText(requireContext(), "Vui lòng nhập đủ thông tin hợp lệ", Toast.LENGTH_SHORT).show()
//                    return@setPositiveButton
//                }
//
//                saveTransaction(type, title, chosen!!, amount)
//                Toast.makeText(requireContext(), "Đã thêm ${vnFormat.format(amount)} đ", Toast.LENGTH_SHORT).show()
//            }
//            .setNegativeButton("Hủy", null)
//            .show()
//    }
//
//
//    /** Lưu xuống users/{uid}/transactions */
//    private fun saveTransaction(type: String, title: String, category: Category, amount: Double) {
//        if (!ensureLoggedIn()) return
//        val userId = auth.currentUser?.uid ?: return
//        val userDoc = db.collection("users").document(userId)
//
//        val txCol = userDoc.collection("transactions")
//        val txId = txCol.document().id
//
//        val t = Transaction(
//            title = title,
//            categoryId = category.id,
//            categoryName = category.name,
//            categoryIconUrl = category.iconUrl, // để TransactionAdapter hiển thị icon
//            amount = amount,
//            type = type,
//            date = Timestamp.now()
//        )
//
//        db.runBatch { batch ->
//            batch.set(txCol.document(txId), t)
//            batch.update(
//                userDoc.collection("categories").document(category.id),
//                "totalAmount",
//                com.google.firebase.firestore.FieldValue.increment(amount.toLong())
//            )
//        }.addOnSuccessListener {
//            Toast.makeText(requireContext(), "Đã lưu giao dịch!", Toast.LENGTH_SHORT).show()
//            // Refresh lại danh sách sau khi thêm
//            loadRecentTransactions()
//        }.addOnFailureListener {
//            Toast.makeText(requireContext(), "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    /** Load chỉ 3 giao dịch gần nhất + cập nhật tổng thu/chi/số dư */
//    private fun loadRecentTransactions() {
//        if (!ensureLoggedIn()) return
//        val userId = auth.currentUser?.uid ?: return
//
//        // Lấy chỉ 3 giao dịch gần nhất
//        db.collection("users").document(userId)
//            .collection("transactions")
//            .orderBy("date", Query.Direction.DESCENDING)
//            .limit(3)
//            .addSnapshotListener { snapshot, error ->
//                if (error != null) {
//                    Toast.makeText(requireContext(), "Lỗi tải dữ liệu: ${error.message}", Toast.LENGTH_SHORT).show()
//                    return@addSnapshotListener
//                }
//
//                transactionList.clear()
//                snapshot?.forEach { doc ->
//                    val t = doc.toObject(Transaction::class.java)
//                    transactionList.add(t)
//                }
//
//                adapter.notifyDataSetChanged()
//                tvCount?.text = "${transactionList.size} giao dịch gần đây"
//            }
//
//        // Tính tổng thu/chi từ TẤT CẢ giao dịch (không giới hạn)
//        calculateTotals(userId)
//    }
//
//    /** Tính tổng thu nhập và chi tiêu từ tất cả giao dịch */
//    private fun calculateTotals(userId: String) {
//        db.collection("users").document(userId)
//            .collection("transactions")
//            .addSnapshotListener { snapshot, error ->
//                if (error != null) return@addSnapshotListener
//
//                var income = 0.0
//                var expense = 0.0
//
//                snapshot?.forEach { doc ->
//                    val t = doc.toObject(Transaction::class.java)
//                    if (t.type.equals("income", true)) {
//                        income += t.amount
//                    } else {
//                        expense += t.amount
//                    }
//                }
//
//                tvIncome?.text = formatVnd(income)
//                tvExpense?.text = formatVnd(expense)
//                tvBalance?.text = formatVnd(income - expense)
//            }
//    }
//
//    /** Setup button "Xem tất cả giao dịch" */
//    private fun setupViewAllButton(view: View) {
//        val btnViewAllTransactions = view.findViewById<MaterialButton>(R.id.btnViewAllTransactions)
//
//        btnViewAllTransactions?.setOnClickListener {
//            val intent = Intent(requireContext(), AllTransactionsActivity::class.java)
//            startActivity(intent)
//
//            // Animation chuyển trang mượt mà
//            requireActivity().overridePendingTransition(
//                android.R.anim.slide_in_left,
//                android.R.anim.slide_out_right
//            )
//        }
//    }
//
//    private fun formatVnd(value: Double): String {
//        val nf = NumberFormat.getInstance(Locale("vi", "VN"))
//        return nf.format(value) + " đ"
//    }
//
//    // ---------- Helpers cho header ----------
//
//    private fun greetingVN(): String {
//        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
//        return when (hour) {
//            in 5..10 -> "Chào buổi sáng! ☀️"
//            in 11..13 -> "Chào buổi trưa! 🍽️"
//            in 14..17 -> "Chào buổi chiều! 🌤️"
//            in 18..22 -> "Chào buổi tối! 🌙"
//            else -> "Xin chào! 👋"
//        }
//    }
//
//    private fun bestNameFromAuth(displayName: String?, email: String?): String {
//        if (!displayName.isNullOrBlank()) return displayName
//        if (!email.isNullOrBlank()) {
//            val local = email.substringBefore("@").trim()
//            if (local.isNotBlank()) return local
//        }
//        return "Người dùng"
//    }
//}

package com.example.quanlychitieu_finly

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
            val clean = edtAmount.text?.toString()?.replace(".", "")?.replace(",", "")?.trim().orEmpty()
            val amount = clean.toDoubleOrNull() ?: 0.0

            if (title.isEmpty() || chosen == null || amount <= 0) {
                Toast.makeText(requireContext(), "Vui lòng nhập đủ thông tin hợp lệ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveTransaction(type, title, chosen!!, amount)
            Toast.makeText(requireContext(), "Đã thêm ${vnFormat.format(amount)} đ", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    // ========== Lưu giao dịch ==========
    private fun saveTransaction(type: String, title: String, category: Category, amount: Double) {
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

    private fun formatVnd(value: Double): String =
        "${NumberFormat.getInstance(Locale("vi", "VN")).format(value)} đ"

    private fun bestNameFromAuth(displayName: String?, email: String?): String {
        if (!displayName.isNullOrBlank()) return displayName
        val name = email?.substringBefore("@") ?: "Người dùng"
        return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }
}
