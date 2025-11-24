package Category

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
import Category.AddCategoryFragment
import com.example.quanlychitieu_finly.R
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CategoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CategoryAdapter
    private lateinit var tabSpending: TextView
    private lateinit var tabIncome: TextView
    private lateinit var tvTransactionCount: TextView
    private lateinit var tvAverage: TextView
    private lateinit var tvHighest: TextView
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
        tvTransactionCount = view.findViewById(R.id.tvTransactionCount)
        tvAverage = view.findViewById(R.id.tvAverage)
        tvHighest = view.findViewById(R.id.tvHighest)

        // RecyclerView + adapter full long-click
        adapter = CategoryAdapter(
            list = emptyList(),
            onClick = { }, // click bình thường
            onLongClick = { category -> showOptions(category) } // nhấn giữ sửa / xoá
        )

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

    private fun showOptions(category: Category) {
        val dialog = android.app.AlertDialog.Builder(requireContext())
        dialog.setTitle("Tuỳ chọn danh mục")
        dialog.setMessage("Bạn muốn làm gì với '${category.name}'?")

        dialog.setPositiveButton("Sửa") { _, _ ->
            val fragment = AddCategoryFragment()
            val b = Bundle()

            b.putString("id", category.id)
            b.putString("name", category.name)
            b.putString("type", category.type)
            b.putString("iconUrl", category.iconUrl)

            fragment.arguments = b

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        dialog.setNegativeButton("Xóa") { _, _ ->
            deleteCategory(category)
        }

        dialog.setNeutralButton("Hủy", null)
        dialog.show()
    }

    private fun deleteCategory(category: Category) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(userId)
            .collection("categories")
            .document(category.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Đã xoá danh mục!", Toast.LENGTH_SHORT).show()
                selectTab(tabSpending)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Xoá thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun selectTab(selectedTab: TextView) {
        val isSpending = selectedTab == tabSpending
        highlightTab(selectedTab)
        adapter.setTab(isSpending)

        val type = if (isSpending) "spending" else "income"
        loadCategoriesFromFirestore(type)
        loadTransactionCount(type)
    }

    private fun loadCategoriesFromFirestore(type: String) {
        val userId = auth.currentUser?.uid ?: return

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

    private fun loadTransactionCount(type: String) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(userId)
            .collection("transactions")
            .whereEqualTo("type", type)
            .get()
            .addOnSuccessListener { docs ->
                val count = docs.size()
                tvTransactionCount.text = count.toString()

                if (count > 0) {
                    val amounts = docs.mapNotNull { it.getDouble("amount") }

                    val total = amounts.sum()
                    val avg = total / count
                    val max = amounts.maxOrNull() ?: 0.0

                    tvAverage.text = "${formatShortNumber(avg)}"
                    tvHighest.text = "${formatShortNumber(max)}"
                } else {
                    tvAverage.text = "0k"
                    tvHighest.text = "0k"
                }
            }
            .addOnFailureListener {
                tvTransactionCount.text = "0"
                tvAverage.text = "0đ"
                tvHighest.text = "0đ"
            }
    }

    private fun formatShortNumber(value: Double): String {
        return when {
            value >= 1_000_000_000 -> String.format("%.1fb", value / 1_000_000_000).replace(".0", "")
            value >= 1_000_000 -> String.format("%.1fm", value / 1_000_000).replace(".0", "")
            value >= 1_000 -> String.format("%.1fk", value / 1_000).replace(".0", "")
            else -> value.toInt().toString()
        }
    }

    private fun highlightTab(selected: TextView) {
        val isSpending = selected == tabSpending

        tabSpending.setTextColor(ContextCompat.getColor(requireContext(), if (isSpending) R.color.bluesky else R.color.gray))
        tabIncome.setTextColor(ContextCompat.getColor(requireContext(), if (!isSpending) R.color.bluesky else R.color.gray))
        tvTotal.setTextColor(ContextCompat.getColor(requireContext(), if (isSpending) R.color.red else R.color.green))
        tvHighest.setTextColor(ContextCompat.getColor(requireContext(), if (isSpending) R.color.red else R.color.green))

        underlineSpending.visibility = if (isSpending) View.VISIBLE else View.INVISIBLE
        underlineIncome.visibility = if (!isSpending) View.VISIBLE else View.INVISIBLE
    }
}
