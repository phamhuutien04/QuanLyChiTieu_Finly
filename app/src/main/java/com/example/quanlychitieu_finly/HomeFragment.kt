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
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.app.DatePickerDialog
import java.util.Calendar

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

    private var selectedDate: Date = Date()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        if (!ensureLoggedIn()) return view

        bindUserHeader(view)

        rvTransactions = view.findViewById(R.id.rvTransactions)

        adapter = TransactionAdapter { transaction ->
        }
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
        val btnSearch = view.findViewById<ImageView>(R.id.btnSearch)
        btnSearch.setOnClickListener { openSearchFriends() }


        val btnNotify = view.findViewById<ImageView>(R.id.notify)
        btnNotify.setOnClickListener { openChatList() }

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

    private fun openSearchFriends() {
        val intent = Intent(requireContext(), SearchFriendsActivity::class.java)
        startActivity(intent)
    }

    private fun openSocialActivity() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        val intent = Intent(requireContext(), SocialActivity::class.java)
        intent.putExtra("profileUid", uid)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        if (ensureLoggedIn()) {
            view?.let { bindUserHeader(it) }
            loadRecentTransactions()
        }
    }

    private fun openChatList() {
        val intent = Intent(requireContext(), ChatListActivity::class.java)
        startActivity(intent)
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
        intent.addFlags(
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        )
        startActivity(intent)
        requireActivity().finish()
    }

    private fun bindUserHeader(root: View) {
        val tvUserName = root.findViewById<TextView>(R.id.tvUserName)

        val user = auth.currentUser ?: return

        tvUserName.text = bestNameFromAuth(user.displayName, user.email)


        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                doc.getString("fullName")?.let { tvUserName.text = it }

            }
    }


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
                    Toast.makeText(requireContext(), "Chưa có danh mục $type", Toast.LENGTH_SHORT)
                        .show()
                    return@addOnSuccessListener
                }
                showAddTransactionSheet(type, categories)
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
        val edtTitle = v.findViewById<TextInputEditText>(R.id.edtTitle)
        val edtCategory = v.findViewById<TextInputEditText>(R.id.edtCategory)
        val edtAmount = v.findViewById<TextInputEditText>(R.id.edtAmount)
        val edtTagFriend = v.findViewById<TextInputEditText>(R.id.edtTagFriend)
        val edtDate = v.findViewById<TextInputEditText>(R.id.edtDate)
        val btnSave = v.findViewById<MaterialButton>(R.id.btnSave)
        val btnCancel = v.findViewById<MaterialButton>(R.id.btnCancel)

        tvTitle.text = if (type == "income") "Thêm thu nhập" else "Thêm chi tiêu"
        var chosen: Category? = categories.firstOrNull()
        edtCategory.setText(chosen?.name ?: "")

        val taggedFriends = mutableListOf<User>()

        selectedDate = Date()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        edtDate.setText(dateFormat.format(selectedDate))

        // 1. CHỌN NGÀY THÁNG
        edtDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = selectedDate

            val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, monthOfYear)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                selectedDate = calendar.time
                edtDate.setText(dateFormat.format(selectedDate))
            }

            DatePickerDialog(
                requireContext(),
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // 2. CHỌN CATEGORY
        edtCategory.setOnClickListener {
            val pick = BottomSheetDialog(requireContext())
            val pv = layoutInflater.inflate(R.layout.bottom_sheet_category_picker, null)
            pick.setContentView(pv)

            val rv = pv.findViewById<RecyclerView>(R.id.rvCategories)
            rv.layoutManager = LinearLayoutManager(requireContext())

            val listUI = categories.map { CatUI(it.id, it.name, it.iconUrl ?: "") }

            // --- SỬA: Truyền ID đang chọn (selectedId) vào Adapter ---
            val currentSelectedId = chosen?.id

            rv.adapter = CategoryPickerAdapter(listUI, currentSelectedId) {
                chosen = categories.find { c -> c.id == it.id }
                edtCategory.setText(it.name)
                pick.dismiss()
            }

            pv.findViewById<MaterialButton>(R.id.btnCancel)?.setOnClickListener { pick.dismiss() }
            pick.show()
        }

        // 3. CHỌN BẠN BÈ
        edtTagFriend.setOnClickListener {
            openFriendPicker(taggedFriends) { newList ->
                taggedFriends.clear()
                taggedFriends.addAll(newList)

                edtTagFriend.setText(
                    if (taggedFriends.isEmpty()) ""
                    else taggedFriends.joinToString { it.username }
                )
            }
        }

        // 4. FORMAT TIỀN
        val vnFormat = NumberFormat.getInstance(Locale("vi", "VN"))
        edtAmount.keyListener = DigitsKeyListener.getInstance("0123456789.,")
        edtAmount.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(18))
        edtAmount.addTextChangedListener(object : TextWatcher {
            var isFormatting = false
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                val raw = s?.toString() ?: return
                val digits = raw.replace(".", "").replace(",", "").replace("[^0-9]".toRegex(), "")
                if (digits.isEmpty()) {
                    isFormatting = true
                    edtAmount.setText("")
                    isFormatting = false
                    return
                }
                val parsed = digits.toDoubleOrNull() ?: return
                val formatted = vnFormat.format(parsed)
                if (formatted != raw) {
                    isFormatting = true
                    edtAmount.setText(formatted)
                    edtAmount.setSelection(formatted.length)
                    isFormatting = false
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val title = edtTitle.text?.toString()?.trim().orEmpty()
            val clean =
                edtAmount.text?.toString()?.replace(".", "")?.replace(",", "")?.trim().orEmpty()
            val amount = clean.toDoubleOrNull() ?: 0.0

            val transactionTimestamp = Timestamp(selectedDate)

            if (title.isEmpty() || chosen == null || amount <= 0) {
                Toast.makeText(requireContext(), "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val currentUid = auth.currentUser!!.uid
            val participantIds = mutableListOf<String>()
            participantIds.add(currentUid)
            participantIds.addAll(taggedFriends.map { it.id }.distinct())

            val perShare = amount / participantIds.size

            saveTransactionForUsers(
                type, title, chosen!!, perShare, participantIds, transactionTimestamp
            )

            dialog.dismiss()
        }

        dialog.show()
    }


    private fun saveTransactionForUsers(
        type: String,
        title: String,
        category: Category,
        amountEach: Double,
        users: List<String>,
        date: Timestamp
    ) {
        val me = auth.currentUser?.uid

        db.runBatch { batch ->
            users.forEach { uid ->
                val userDoc = db.collection("users").document(uid)
                val txCol = userDoc.collection("transactions")
                val txId = txCol.document().id

                val tx = Transaction(
                    title = title,
                    categoryId = category.id,
                    categoryName = category.name,
                    categoryIconUrl = category.iconUrl,
                    amount = amountEach,
                    type = type,
                    date = date
                )

                batch.set(txCol.document(txId), tx)

                if (uid == me) {
                    batch.update(
                        userDoc.collection("categories").document(category.id),
                        "totalAmount",
                        FieldValue.increment(amountEach)
                    )
                }
            }
        }.addOnSuccessListener {
            val myId = auth.currentUser?.uid ?: return@addOnSuccessListener
            loadRecentTransactionsForUser(myId)
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ============================================================
    // BOTTOM SHEET CHỌN BẠN BÈ
    // ============================================================
    private fun openFriendPicker(
        currentSelected: List<User>,
        onDone: (List<User>) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return

        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_pick_friend, null)
        dialog.setContentView(view)

        val rv = view.findViewById<RecyclerView>(R.id.rvFriends)
        val btnDone = view.findViewById<MaterialButton>(R.id.btnDone)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)

        val friends = mutableListOf<User>()

        db.collection("users").document(uid)
            .collection("friends")
            .get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    Toast.makeText(requireContext(), "Bạn chưa có bạn bè", Toast.LENGTH_SHORT)
                        .show()
                    dialog.dismiss()
                    return@addOnSuccessListener
                }

                val friendIds = snap.documents.map { it.id }

                db.collection("users")
                    .whereIn(FieldPath.documentId(), friendIds)
                    .get()
                    .addOnSuccessListener { userSnap ->
                        friends.clear()
                        for (u in userSnap.documents) {
                            friends.add(
                                User(
                                    id = u.id,
                                    username = u.getString("username") ?: "",
                                    email = u.getString("email") ?: "",
                                    avatarUrl = u.getString("avatarUrl") ?: ""
                                )
                            )
                        }

                        rv.layoutManager = LinearLayoutManager(requireContext())
                        rv.adapter = FriendPickerAdapter(friends, currentSelected)
                    }
            }

        btnDone.setOnClickListener {
            val adapter = rv.adapter as? FriendPickerAdapter
            if (adapter != null) {
                onDone(adapter.getSelectedList())
            }
            dialog.dismiss()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    // ============================================================
    // LOAD GIAO DỊCH & TỔNG
    // ============================================================
    private fun loadRecentTransactions() {
        val userId = auth.currentUser?.uid ?: return
        loadRecentTransactionsForUser(userId)
        calculateTotals(userId)
    }

    private fun loadRecentTransactionsForUser(userId: String) {
        db.collection("users").document(userId)
            .collection("transactions")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(3) // Lấy 3 giao dịch gần nhất
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                transactionList.clear()
                snapshot?.forEach { doc ->
                    transactionList.add(doc.toObject(Transaction::class.java))
                }

                // Hàm setData sẽ tự động gom nhóm theo ngày
                adapter.setData(transactionList)

                tvCount?.text = "${transactionList.size} giao dịch gần đây"
            }
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
        view.findViewById<MaterialButton>(R.id.btnViewAllTransactions)
            ?.setOnClickListener {
                startActivity(
                    Intent(
                        requireContext(),
                        AllTransactionsActivity::class.java
                    )
                )
                requireActivity().overridePendingTransition(
                    android.R.anim.slide_in_left,
                    android.R.anim.slide_out_right
                )
            }
    }

    private fun formatVnd(value: Double): String =
        "${NumberFormat.getInstance(Locale("vi", "VN")).format(value)} đ"

    private fun bestNameFromAuth(displayName: String?, email: String?): String {
        if (!displayName.isNullOrBlank()) return displayName
        val name = email?.substringBefore("@") ?: "Người dùng"
        return name.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }
    }
}