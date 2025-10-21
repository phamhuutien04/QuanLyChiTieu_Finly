//package com.example.quanlychitieu_finly
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ArrayAdapter
//import android.widget.AutoCompleteTextView
//import android.widget.EditText
//import android.widget.TextView
//import android.widget.Toast
//import androidx.fragment.app.Fragment
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.google.android.material.card.MaterialCardView
//import com.google.android.material.dialog.MaterialAlertDialogBuilder
//import com.google.firebase.Timestamp
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.firestore.Query
//import java.text.NumberFormat
//import java.text.SimpleDateFormat
//import java.util.*
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
//        btnAddExpense.setOnClickListener { openAddTransactionFlow("expense") }
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
//        // Load tất cả giao dịch (users/{uid}/transactions)
//        subscribeTransactions()
//
//        return view
//    }
//
//    /** 1) Lấy categories -> 2) Mở dialog nhập & chọn danh mục -> 3) Lưu transaction */
//    private fun openAddTransactionFlow(type: String) {
//        val userId = auth.currentUser?.uid ?: run {
//            Toast.makeText(requireContext(), "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val wantedType = if (type == "income") "income" else "spending"
//
//        db.collection("users").document(userId)
//            .collection("categories")
//            .whereEqualTo("type", wantedType)              // <-- chỉ lấy danh mục đúng loại
//            .orderBy("name")                               // sắp xếp theo tên (optional)
//            .get()
//            .addOnSuccessListener { snap ->
//                val categories = snap.documents.mapNotNull { doc ->
//                    doc.toObject(Category::class.java)?.apply { id = doc.id }  // <-- gắn id
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
//        // chọn sẵn mục đầu
//        var chosen: Category? = categories.firstOrNull()
//        actv.setText(chosen?.name ?: "", false)
//
//        // 🔧 QUAN TRỌNG: khi chọn item, set lại text = name (không phải toString())
//        actv.setOnItemClickListener { _, _, position, _ ->
//            chosen = categories[position]
//            actv.setText(chosen!!.name, false)
//        }
//
//        MaterialAlertDialogBuilder(requireContext())
//            .setTitle(if (type == "income") "Thêm thu nhập" else "Thêm chi tiêu")
//            .setView(dialogView)
//            .setPositiveButton("Lưu") { _, _ ->
//                val title = edtTitle.text.toString().trim()
//                val amount = edtAmount.text.toString().trim().toDoubleOrNull() ?: 0.0
//
//                // fallback nếu người dùng tự sửa text
//                if (chosen == null) {
//                    val typed = actv.text?.toString()?.trim().orEmpty()
//                    chosen = categories.find { it.name == typed }
//                }
//
//                if (title.isEmpty() || chosen == null || amount <= 0) {
//                    Toast.makeText(requireContext(), "Vui lòng nhập đủ thông tin hợp lệ", Toast.LENGTH_SHORT).show()
//                    return@setPositiveButton
//                }
//                saveTransaction(type, title, chosen!!, amount)
//            }
//            .setNegativeButton("Hủy", null)
//            .show()
//    }
//
//
//    /** Lưu xuống users/{uid}/transactions */
//    private fun saveTransaction(type: String, title: String, category: Category, amount: Double) {
//        val userId = auth.currentUser?.uid ?: return
//        val userDoc = db.collection("users").document(userId)
//
//        // Tạo id trước để gắn vào doc (dễ xóa/sửa sau này)
//        val txCol = userDoc.collection("transactions")
//        val txId = txCol.document().id
//
//        val t = Transaction(
//            title = title,
//            categoryId = category.id,
//            categoryName = category.name,
//            categoryIconUrl = category.iconUrl,
//            categoryColorHex = "",              // có color thì set vào đây
//            amount = amount,
//            type = type,
//            date = Timestamp.now()
//            // nếu muốn lưu luôn id: thêm field vào model Transaction rồi set = txId
//        )
//
//        // Dùng batch để vừa thêm giao dịch, vừa cộng dồn totalAmount
//        db.runBatch { batch ->
//            batch.set(txCol.document(txId), t)
//
//            // Nếu Category.totalAmount là Long:
//            batch.update(
//                userDoc.collection("categories").document(category.id),
//                "totalAmount",
//                com.google.firebase.firestore.FieldValue.increment(amount.toLong()) // hoặc amount nếu dùng Double
//            )
//        }.addOnSuccessListener {
//            Toast.makeText(requireContext(), "Đã lưu giao dịch!", Toast.LENGTH_SHORT).show()
//        }.addOnFailureListener {
//            Toast.makeText(requireContext(), "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//
//    /** Lắng nghe realtime tất cả giao dịch -> cập nhật list + tổng */
//    private fun subscribeTransactions() {
//        val userId = auth.currentUser?.uid ?: return
//
//        db.collection("users").document(userId)
//            .collection("transactions")
//            .orderBy("date", Query.Direction.DESCENDING)
//            .addSnapshotListener { snapshot, error ->
//                if (error != null) {
//                    Toast.makeText(requireContext(), "Lỗi tải dữ liệu: ${error.message}", Toast.LENGTH_SHORT).show()
//                    return@addSnapshotListener
//                }
//
//                transactionList.clear()
//                var income = 0.0
//                var expense = 0.0
//
//                snapshot?.forEach { doc ->
//                    val t = doc.toObject(Transaction::class.java)
//                    transactionList.add(t)
//                    if (t.type == "income") income += t.amount else expense += t.amount
//                }
//
//                adapter.notifyDataSetChanged()
//                tvCount?.text = "${transactionList.size} giao dịch"
//                tvIncome?.text = formatVnd(income)
//                tvExpense?.text = formatVnd(expense)
//                tvBalance?.text = formatVnd(income - expense)
//            }
//    }
//
//    private fun formatVnd(value: Double): String {
//        val nf = NumberFormat.getInstance(Locale("vi", "VN"))
//        return nf.format(value) + " đ"
//    }
//}
//


package com.example.quanlychitieu_finly

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

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

        // Nếu chưa đăng nhập -> chuyển LoginActivity và dừng ở đây
        if (!ensureLoggedIn()) return view

        // Header: tên, avatar, lời chào
        bindUserHeader(view)

        // RecyclerView
        rvTransactions = view.findViewById(R.id.rvTransactions)
        adapter = TransactionAdapter(transactionList)
        rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        rvTransactions.adapter = adapter

        // Buttons (MaterialCardView)
        val btnAddIncome: MaterialCardView = view.findViewById(R.id.add_income_layout)
        val btnAddExpense: MaterialCardView = view.findViewById(R.id.add_expense_layout)
        btnAddIncome.setOnClickListener { openAddTransactionFlow("income") }
        btnAddExpense.setOnClickListener { openAddTransactionFlow("expense") }

        // Tổng
        tvIncome = view.findViewById(R.id.tvIncome)
        tvExpense = view.findViewById(R.id.tvExpense)
        tvBalance = view.findViewById(R.id.tvBalance)
        tvCount   = view.findViewById(R.id.tvTransactionCount)

        // Ngày
        val tvDate = view.findViewById<TextView>(R.id.tvDate)
        val df = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("vi", "VN"))
        tvDate.text = df.format(Date())

        // Load tất cả giao dịch (users/{uid}/transactions)
        subscribeTransactions()

        return view
    }

    override fun onResume() {
        super.onResume()
        // Nếu quay lại fragment mà user đã đăng xuất -> LoginActivity
        if (ensureLoggedIn()) {
            // Refresh lại tên & avatar (phòng khi user vừa cập nhật hồ sơ)
            view?.let { bindUserHeader(it) }
        }
    }

    /** Trả về true nếu đã đăng nhập; nếu chưa thì chuyển sang LoginActivity và trả false */
    private fun ensureLoggedIn(): Boolean {
        val user = auth.currentUser
        return if (user == null) {
            redirectToLogin()
            false
        } else {
            true
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        requireActivity().finish()
    }

    /** Bind tên thật + avatar + lời chào vào header */
    private fun bindUserHeader(root: View) {
        val tvUserName = root.findViewById<TextView>(R.id.tvUserName)
        val imgAvatar  = root.findViewById<ImageView>(R.id.imgAvatar)
        val tvGreeting = root.findViewById<TextView>(R.id.tvGreeting)

        val user = auth.currentUser ?: return

        // Lời chào theo giờ
        tvGreeting.text = greetingVN()

        // Hiển thị tạm thời từ Auth để UI không trễ
        tvUserName.text = bestNameFromAuth(user.displayName, user.email)

        // Ảnh tạm thời từ Auth (nếu có)
        user.photoUrl?.let { uri ->
            Glide.with(requireContext())
                .load(uri)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_manage)
                .into(imgAvatar)
        }

        // Override bằng hồ sơ Firestore nếu có
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val fullName  = doc.getString("fullName")
                val username  = doc.getString("username")
                val avatarUrl = doc.getString("avatarUrl")

                val chosenName = when {
                    !fullName.isNullOrBlank() -> fullName
                    !username.isNullOrBlank() -> username
                    else -> null
                }
                if (!chosenName.isNullOrBlank()) {
                    tvUserName.text = chosenName
                }

                if (!avatarUrl.isNullOrBlank()) {
                    Glide.with(requireContext())
                        .load(avatarUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_manage)
                        .into(imgAvatar)
                }
            }
            .addOnFailureListener {
                // Giữ lại tên/ảnh từ Auth nếu Firestore lỗi
            }
    }

    /** 1) Lấy categories -> 2) Mở dialog nhập & chọn danh mục -> 3) Lưu transaction */
    private fun openAddTransactionFlow(type: String) {
        if (!ensureLoggedIn()) return
        val userId = auth.currentUser?.uid ?: return

        val wantedType = if (type == "income") "income" else "spending"

        db.collection("users").document(userId)
            .collection("categories")
            .whereEqualTo("type", wantedType)
            .orderBy("name")
            .get()
            .addOnSuccessListener { snap ->
                val categories = snap.documents.mapNotNull { doc ->
                    doc.toObject(Category::class.java)?.apply { id = doc.id }
                }

                if (categories.isEmpty()) {
                    Toast.makeText(requireContext(), "Chưa có danh mục $wantedType. Hãy tạo trước.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                showAddTransactionDialog(type, categories)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Lỗi tải danh mục: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /** Hiển thị dialog nhập title/amount + chọn category từ list */
    private fun showAddTransactionDialog(type: String, categories: List<Category>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_transaction, null)
        val edtTitle   = dialogView.findViewById<EditText>(R.id.edtTitle)
        val edtAmount  = dialogView.findViewById<EditText>(R.id.edtAmount)
        val actv       = dialogView.findViewById<AutoCompleteTextView>(R.id.actvCategory)

        val catAdapter = CategoryDropdownAdapter(requireContext(), categories)
        actv.setAdapter(catAdapter)

        var chosen: Category? = categories.firstOrNull()
        actv.setText(chosen?.name ?: "", false)

        actv.setOnItemClickListener { _, _, position, _ ->
            chosen = categories[position]
            actv.setText(chosen!!.name, false)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (type == "income") "Thêm thu nhập" else "Thêm chi tiêu")
            .setView(dialogView)
            .setPositiveButton("Lưu") { _, _ ->
                val title = edtTitle.text.toString().trim()
                val amount = edtAmount.text.toString().trim().toDoubleOrNull() ?: 0.0

                if (chosen == null) {
                    val typed = actv.text?.toString()?.trim().orEmpty()
                    chosen = categories.find { it.name == typed }
                }

                if (title.isEmpty() || chosen == null || amount <= 0) {
                    Toast.makeText(requireContext(), "Vui lòng nhập đủ thông tin hợp lệ", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                saveTransaction(type, title, chosen!!, amount)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    /** Lưu xuống users/{uid}/transactions */
    private fun saveTransaction(type: String, title: String, category: Category, amount: Double) {
        if (!ensureLoggedIn()) return
        val userId = auth.currentUser?.uid ?: return
        val userDoc = db.collection("users").document(userId)

        val txCol = userDoc.collection("transactions")
        val txId = txCol.document().id

        val t = Transaction(
            title = title,
            categoryId = category.id,
            categoryName = category.name,
            categoryIconUrl = category.iconUrl, // để TransactionAdapter hiển thị icon
            categoryColorHex = "",              // nếu bạn có colorHex ở Category thì set tại đây
            amount = amount,
            type = type,
            date = Timestamp.now()
        )

        db.runBatch { batch ->
            batch.set(txCol.document(txId), t)
            batch.update(
                userDoc.collection("categories").document(category.id),
                "totalAmount",
                com.google.firebase.firestore.FieldValue.increment(amount.toLong())
            )
        }.addOnSuccessListener {
            Toast.makeText(requireContext(), "Đã lưu giao dịch!", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /** Lắng nghe realtime tất cả giao dịch -> cập nhật list + tổng */
    private fun subscribeTransactions() {
        if (!ensureLoggedIn()) return
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("transactions")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(requireContext(), "Lỗi tải dữ liệu: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                transactionList.clear()
                var income = 0.0
                var expense = 0.0

                snapshot?.forEach { doc ->
                    val t = doc.toObject(Transaction::class.java)
                    transactionList.add(t)
                    if (t.type.equals("income", true)) income += t.amount else expense += t.amount
                }

                adapter.notifyDataSetChanged()
                tvCount?.text = "${transactionList.size} giao dịch"
                tvIncome?.text = formatVnd(income)
                tvExpense?.text = formatVnd(expense)
                tvBalance?.text = formatVnd(income - expense)
            }
    }

    private fun formatVnd(value: Double): String {
        val nf = NumberFormat.getInstance(Locale("vi", "VN"))
        return nf.format(value) + " đ"
    }

    // ---------- Helpers cho header ----------

    private fun greetingVN(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..10 -> "Chào buổi sáng! ☀️"
            in 11..13 -> "Chào buổi trưa! 🍽️"
            in 14..17 -> "Chào buổi chiều! 🌤️"
            in 18..22 -> "Chào buổi tối! 🌙"
            else -> "Xin chào! 👋"
        }
    }

    private fun bestNameFromAuth(displayName: String?, email: String?): String {
        if (!displayName.isNullOrBlank()) return displayName
        if (!email.isNullOrBlank()) {
            val local = email.substringBefore("@").trim()
            if (local.isNotBlank()) return local
        }
        return "Người dùng"
    }
}

