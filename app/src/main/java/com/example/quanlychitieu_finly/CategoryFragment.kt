package com.example.quanlychitieu_finly

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CategoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CategoryAdapter
    private lateinit var tabSpending: TextView
    private lateinit var tabIncome: TextView
    private lateinit var tvTotal: TextView
    private lateinit var underlineSpending: View
    private lateinit var underlineIncome: View
    private lateinit var fabAddCategory: ExtendedFloatingActionButton

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_category, container, false)

        // Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Ánh xạ view
        recyclerView = view.findViewById(R.id.rcvCategories)
        tabSpending = view.findViewById(R.id.tabSpending)
        tabIncome = view.findViewById(R.id.tabIncome)
        underlineSpending = view.findViewById(R.id.underlineSpending)
        underlineIncome = view.findViewById(R.id.underlineIncome)
        tvTotal = view.findViewById(R.id.tvTotal)
        fabAddCategory = view.findViewById(R.id.fabAddCategory)

        // RecyclerView
        adapter = CategoryAdapter(emptyList()) {}
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter

        // Mặc định tab Chi tiêu
        selectTab(tabSpending)

        tabSpending.setOnClickListener { selectTab(tabSpending) }
        tabIncome.setOnClickListener { selectTab(tabIncome) }

        fabAddCategory.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AddCategoryFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    private fun selectTab(selectedTab: TextView) {
        val isSpending = selectedTab == tabSpending
        highlightTab(selectedTab)
        adapter.setTab(isSpending)

        val type = if (isSpending) "spending" else "income"
        loadCategoriesFromFirestore(type)
    }

    private fun loadCategoriesFromFirestore(type: String) {
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(context, "Người dùng chưa đăng nhập!", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users").document(userId)
            .collection("categories")
            .whereEqualTo("type", type)
            .orderBy("name", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { docs ->
                val list = docs.toObjects(Category::class.java)
                adapter.updateData(list)
                updateTotal(list)
                if (list.isEmpty()) Toast.makeText(context, "Không có danh mục nào", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Lỗi tải danh mục: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateTotal(list: List<Category>) {
        val total = list.sumOf { it.totalAmount }
        tvTotal.text = "%,dđ".format(total)
    }

    private fun highlightTab(selected: TextView) {
        val isSpending = selected == tabSpending
        tabSpending.setTextColor(ContextCompat.getColor(requireContext(), if (isSpending) R.color.bluesky else R.color.gray))
        tabIncome.setTextColor(ContextCompat.getColor(requireContext(), if (!isSpending) R.color.bluesky else R.color.gray))
        tvTotal.setTextColor(ContextCompat.getColor(requireContext(), if (isSpending) R.color.red else R.color.green))
        underlineSpending.visibility = if (isSpending) View.VISIBLE else View.INVISIBLE
        underlineIncome.visibility = if (!isSpending) View.VISIBLE else View.INVISIBLE
    }
}
