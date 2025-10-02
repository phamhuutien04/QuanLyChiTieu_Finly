package com.example.quanlychitieu_finly

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class CategoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CategoryAdapter
    private lateinit var tabSpending: TextView
    private lateinit var tabIncome: TextView
    private lateinit var tvTotal: TextView
    private lateinit var underlineSpending: View
    private lateinit var underlineIncome: View
    private lateinit var fabAddCategory: ExtendedFloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_category, container, false)

        // Ánh xạ view
        recyclerView = view.findViewById(R.id.rcvCategories)
        tabSpending = view.findViewById(R.id.tabSpending)
        tabIncome = view.findViewById(R.id.tabIncome)
        underlineSpending = view.findViewById(R.id.underlineSpending)
        underlineIncome = view.findViewById(R.id.underlineIncome)
        tvTotal = view.findViewById(R.id.tvTotal)
        fabAddCategory = view.findViewById(R.id.fabAddCategory)

        // Danh mục Chi tiêu
        val spendingCategories = listOf(
            Category(R.drawable.ic_food, "Ăn uống", "2,000,000đ"),
            Category(R.drawable.ic_car, "Di chuyển", "800,000đ"),
            Category(R.drawable.ic_shop, "Mua sắm", "1,500,000đ"),
            Category(R.drawable.ic_bill, "Hóa đơn", "1,200,000đ"),
            Category(R.drawable.ic_sk, "Y tế", "2,000,000đ"),
            Category(R.drawable.ic_cinema, "Giải trí", "800,000đ"),
            Category(R.drawable.ic_sports, "Thể thao", "1,500,000đ"),
            Category(R.drawable.ic_adds, "Khác", "2,000,000đ")
        )

        // Danh mục Thu nhập
        val incomeCategories = listOf(
            Category(R.drawable.ic_wage, "Lương", "10,000,000đ"),
            Category(R.drawable.ic_wages, "Thưởng", "2,000,000đ"),
            Category(R.drawable.ic_adds, "Quà tặng", "2,000,000đ"),
            Category(R.drawable.ic_adds, "Khác", "2,000,000đ")
        )

        // Mặc định hiển thị Chi tiêu
        showCategoryList(spendingCategories)
        updateTotal(spendingCategories)
        highlightTab(tabSpending)
        adapter.setTab(true)

        // Chuyển tab → Chi tiêu
        tabSpending.setOnClickListener {
            showCategoryList(spendingCategories)
            updateTotal(spendingCategories)
            highlightTab(tabSpending)
            adapter.setTab(true) // Chi tiêu → đỏ
        }

        // Chuyển tab → Thu nhập
        tabIncome.setOnClickListener {
            showCategoryList(incomeCategories)
            updateTotal(incomeCategories)
            highlightTab(tabIncome)
            adapter.setTab(false) // Thu nhập → xanh
        }

//         Ấn nút Thêm → sang fragment khác
        fabAddCategory.setOnClickListener {
            val newFragment = AddCategoryFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, newFragment) // FrameLayout chứa fragment
                .addToBackStack(null) // để nhấn Back quay lại
                .commit()
        }

        return view
    }

    // Hiển thị danh sách Category
    private fun showCategoryList(list: List<Category>) {
        adapter = CategoryAdapter(list) { category ->
            // TODO: xử lý khi click item nếu cần
        }
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter
    }

    // Tính tổng số tiền
    private fun updateTotal(list: List<Category>) {
        var total = 0L
        for (c in list) {
            val amount = c.amount
            if (!amount.isNullOrBlank()) {
                val value = amount.replace(",", "").replace("đ", "").trim().toLong()
                total += value
            }
        }
        tvTotal.text = "%,dđ".format(total)
    }

    // Đổi màu tab + underline
    private fun highlightTab(selected: TextView) {
        if (selected == tabSpending) {
            tabSpending.setTextColor(ContextCompat.getColor(requireContext(), R.color.bluesky))
            tabIncome.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
            tvTotal.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))

            underlineSpending.visibility = View.VISIBLE
            underlineIncome.visibility = View.INVISIBLE
        } else {
            tvTotal.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
            tabIncome.setTextColor(ContextCompat.getColor(requireContext(), R.color.bluesky))
            tabSpending.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
            underlineIncome.visibility = View.VISIBLE
            underlineSpending.visibility = View.INVISIBLE
        }
    }
}
