package com.example.quanlychitieu_finly

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DEPRECATION")
class HomeFragment : Fragment() {

    private lateinit var rvTransactions: RecyclerView
    private lateinit var adapter: TransactionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Nút thêm giao dịch (FloatingActionButton)


        // RecyclerView hiển thị danh sách giao dịch
        rvTransactions = view.findViewById(R.id.rvTransactions)

        val transactions = listOf(
            Transaction("Ăn trưa", "Ăn uống", "-50.000 đ", "18/09/2025"),
            Transaction("Lương tháng 12", "Lương", "+2.000.000 đ", "18/09/2025"),
            Transaction("Xăng xe", "Di chuyển", "-300.000 đ", "17/09/2025")
        )

        adapter = TransactionAdapter(transactions)
        rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        rvTransactions.adapter = adapter

        // Set ngày hiện tại
        val tvDate = view.findViewById<android.widget.TextView>(R.id.tvDate)
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("vi", "VN"))
        tvDate.text = dateFormat.format(Date())

        return view
    }
}
