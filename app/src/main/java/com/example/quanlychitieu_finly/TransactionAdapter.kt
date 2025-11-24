package com.example.quanlychitieu_finly

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/** MODEL NHÓM NGÀY */
data class DailyGroup(
    val date: Date,
    val totalAmount: Double,
    val transactions: List<Transaction>
)

/** ADAPTER CHÍNH */
class TransactionAdapter(
    private val transactionList: MutableList<Transaction>,
    private val onItemClick: (Transaction) -> Unit = {}
) : RecyclerView.Adapter<TransactionAdapter.DayViewHolder>() {

    private val groupedList = mutableListOf<DailyGroup>()
    private val originalTransactions = mutableListOf<Transaction>()

    private val fallbackColors = mapOf(
        "Ăn uống" to "#FF6B6B",
        "Di chuyển" to "#4ECDC4",
        "Mua sắm" to "#95E1D3",
        "Giải trí" to "#F38181",
        "Học tập" to "#AA96DA",
        "Y tế" to "#FCBAD3",
        "Hóa đơn" to "#A8D8EA",
        "Lương" to "#4CAF50",
        "Thưởng" to "#81C784",
        "Đầu tư" to "#FFD54F",
        "Khác" to "#B0BEC5"
    )

    /** VIEW HOLDER CHO NGÀY */
    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvHeaderDate: TextView = itemView.findViewById(R.id.tvHeaderDate)
        val tvHeaderDayOfWeek: TextView = itemView.findViewById(R.id.tvHeaderDayOfWeek)
        val tvHeaderMonthYear: TextView = itemView.findViewById(R.id.tvHeaderMonthYear)
        val tvHeaderTotal: TextView = itemView.findViewById(R.id.tvHeaderTotal)
        val itemContainer: LinearLayout = itemView.findViewById(R.id.itemContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_date_header, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val group = groupedList[position]
        val context = holder.itemView.context
        val vnLocale = Locale("vi", "VN")
        val nf = NumberFormat.getInstance(vnLocale)

        /** HEADER NGÀY */
        val cal = Calendar.getInstance()
        cal.time = group.date

        holder.tvHeaderDate.text = cal.get(Calendar.DAY_OF_MONTH).toString()

        val dayFormat = SimpleDateFormat("EEEE", vnLocale)
        holder.tvHeaderDayOfWeek.text = dayFormat
            .format(group.date)
            .replaceFirstChar { it.uppercase() }

        val monthFormat = SimpleDateFormat("'tháng' MM yyyy", vnLocale)
        holder.tvHeaderMonthYear.text = monthFormat.format(group.date)

        holder.tvHeaderTotal.text = nf.format(group.totalAmount)

        /** CLEAR VIEWS CŨ */
        holder.itemContainer.removeAllViews()
        val inflater = LayoutInflater.from(context)

        /** ADD TRANSACTION ITEM */
        group.transactions.forEach { t ->
            val itemView = inflater.inflate(R.layout.item_transaction, holder.itemContainer, false)

            val tvTitle = itemView.findViewById<TextView>(R.id.tvTitle)
            val tvCategory = itemView.findViewById<TextView>(R.id.tvCategory)
            val tvAmount = itemView.findViewById<TextView>(R.id.tvAmount)
            val tvDate = itemView.findViewById<TextView>(R.id.tvDate)
            val iconBg = itemView.findViewById<FrameLayout>(R.id.iconBackground)
            val ivIcon = itemView.findViewById<ImageView>(R.id.ivCategoryIcon)

            tvTitle.text = t.title
            tvCategory.text = t.categoryName

            val isIncome = t.type.equals("income", true)
            val sign = if (isIncome) "+" else "-"
            tvAmount.text = "$sign${nf.format(t.amount)} đ"
            tvAmount.setTextColor(
                Color.parseColor(if (isIncome) "#34C759" else "#FF3B30")
            )

            val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            tvDate.text = df.format(t.date.toDate())

            val colorHex = fallbackColors[t.categoryName] ?: "#B0BEC5"
            iconBg.setBackgroundColor(Color.parseColor(colorHex))

            ImageViewCompat.setImageTintList(ivIcon, null)

            val url = t.categoryIconUrl
            if (url.startsWith("http")) {
                Glide.with(context).load(url).into(ivIcon)
            } else {
                val resId = context.resources.getIdentifier(
                    url, "drawable", context.packageName
                )
                ivIcon.setImageResource(if (resId != 0) resId else android.R.drawable.ic_menu_gallery)
            }

            /** CLICK */
            itemView.setOnClickListener { onItemClick(t) }

            holder.itemContainer.addView(itemView)
        }
    }

    override fun getItemCount(): Int = groupedList.size

    /** SET DỮ LIỆU & NHÓM THEO NGÀY */
    fun setData(newTransactions: List<Transaction>) {
        originalTransactions.clear()
        originalTransactions.addAll(newTransactions)
        groupedList.clear()

        if (newTransactions.isNotEmpty()) {
            val sorted = newTransactions.sortedByDescending { it.date.toDate() }

            val groupedMap = sorted.groupBy {
                SimpleDateFormat("yyyyMMdd", Locale.getDefault())
                    .format(it.date.toDate())
            }

            groupedMap.forEach { (_, txs) ->
                val total = txs.sumOf {
                    if (it.type == "income") it.amount else -it.amount
                }
                groupedList.add(
                    DailyGroup(
                        txs[0].date.toDate(),
                        total,
                        txs
                    )
                )
            }
        }
        notifyDataSetChanged()
    }

    /** CẬP NHẬT / XÓA / THAY THẾ */
    fun replaceAll(newItems: List<Transaction>) {
        transactionList.clear()
        transactionList.addAll(newItems)
        setData(newItems)
    }

    fun updateAt(position: Int, newItem: Transaction) {
        transactionList[position] = newItem
        setData(transactionList)
    }

    fun removeAt(position: Int) {
        transactionList.removeAt(position)
        setData(transactionList)
    }
}
