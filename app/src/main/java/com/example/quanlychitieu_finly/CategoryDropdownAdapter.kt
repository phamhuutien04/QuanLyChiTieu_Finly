package com.example.quanlychitieu_finly

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide

class CategoryDropdownAdapter(
    context: Context,
    private val items: List<Category>
) : ArrayAdapter<Category>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
        bind(position, convertView, parent)

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
        bind(position, convertView, parent)

    private fun bind(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_category_dropdown, parent, false)

        val ivIcon = view.findViewById<ImageView>(R.id.ivIcon)
        val tvName = view.findViewById<TextView>(R.id.tvName)

        val item = items[position]
        tvName.text = item.name

        // iconUrl: nếu là URL -> Glide; nếu là tên drawable -> load local
        val url = item.iconUrl?.trim().orEmpty()
        if (url.startsWith("http", ignoreCase = true)) {
            Glide.with(context)
                .load(url)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(ivIcon)
        } else {
            // cho phép lưu "ic_food", "ic_salary" ... trong iconUrl
            val resId = getDrawableIdByName(url)
            if (resId != 0) ivIcon.setImageResource(resId)
            else ivIcon.setImageResource(android.R.drawable.ic_menu_gallery)
        }
        return view
    }

    private fun getDrawableIdByName(name: String): Int {
        if (name.isBlank()) return 0
        return context.resources.getIdentifier(name, "drawable", context.packageName)
    }
}
