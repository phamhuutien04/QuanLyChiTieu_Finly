package Category

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.quanlychitieu_finly.R

class CategoryAdapter(
    private var list: List<Category>,
    private val onClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    private var isSpendingTab = true

    fun setTab(isSpending: Boolean) {
        isSpendingTab = isSpending
        notifyDataSetChanged()
    }

    fun updateData(newList: List<Category>) {
        list = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = list[position]
        val context = holder.itemView.context

        holder.tvName.text = category.name
        holder.tvAmount.text = "%,dđ".format(category.totalAmount)

        val mainColorRes = if (isSpendingTab) R.color.red else R.color.green
        val mainColor = ContextCompat.getColor(context, mainColorRes)
        val blackColor = ContextCompat.getColor(context, R.color.black)

        holder.tvName.setTextColor(blackColor)
        holder.tvAmount.setTextColor(mainColor)


        Glide.with(context)
            .load(category.iconUrl)
            .placeholder(R.drawable.ic_loading)
            .error(R.drawable.ic_loading)
            .into(holder.ivIcon)

        holder.itemView.setOnClickListener { onClick(category) }
    }

    // ===== ViewHolder =====
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
        val tvName: TextView = itemView.findViewById(R.id.tvCategoryName)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
    }
}