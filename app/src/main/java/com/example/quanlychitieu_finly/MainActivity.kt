package com.example.quanlychitieu_finly

import Category.CategoryFragment
import Settings.SettingsFragment
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    // Bottom navigation tabs
    private lateinit var tabHome: FrameLayout
    private lateinit var tabCategorys: FrameLayout
    private lateinit var tabAnalytics: FrameLayout
    private lateinit var tabSettings: FrameLayout

    // Floating action button
    private lateinit var btnAdd: FloatingActionButton

    // Indicators (thanh xanh phía trên)
    private lateinit var indicatorHome: View
    private lateinit var indicatorCategory: View
    private lateinit var indicatorAnalytics: View
    private lateinit var indicatorSettings: View

    // Icons
    private lateinit var iconHome: ImageView
    private lateinit var iconCategory: ImageView
    private lateinit var iconAnalytics: ImageView
    private lateinit var iconSettings: ImageView

    // Texts
    private lateinit var textHome: TextView
    private lateinit var textCategory: TextView
    private lateinit var textAnalytics: TextView
    private lateinit var textSettings: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Đổi màu status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = ContextCompat.getColor(this, R.color.bluesky)
        }

        initViews()
        setupClickListeners()

        // Mặc định mở Home khi mới vào app
        selectTab(0)
    }

    private fun initViews() {
        // Tabs
        tabHome = findViewById(R.id.tab_home)
        tabCategorys = findViewById(R.id.tab_categorys)
        tabAnalytics = findViewById(R.id.tab_analytics)
        tabSettings = findViewById(R.id.tab_settings)

        // Indicators
        indicatorHome = findViewById(R.id.indicator_home)
        indicatorCategory = findViewById(R.id.indicator_category)
        indicatorAnalytics = findViewById(R.id.indicator_analytics)
        indicatorSettings = findViewById(R.id.indicator_settings)

        // Icons
        iconHome = findViewById(R.id.icon_home)
        iconCategory = findViewById(R.id.icon_category)
        iconAnalytics = findViewById(R.id.icon_analytics)
        iconSettings = findViewById(R.id.icon_settings)

        // Texts
        textHome = findViewById(R.id.text_home)
        textCategory = findViewById(R.id.text_category)
        textAnalytics = findViewById(R.id.text_analytics)
        textSettings = findViewById(R.id.text_settings)
    }

    private fun setupClickListeners() {
        tabHome.setOnClickListener { selectTab(0) }
        tabCategorys.setOnClickListener { selectTab(1) }
        tabAnalytics.setOnClickListener { selectTab(2) }
        tabSettings.setOnClickListener { selectTab(3) }
    }

    private fun selectTab(position: Int) {
        // Animate indicators - thanh xanh phía trên
        animateIndicator(indicatorHome, position == 0)
        animateIndicator(indicatorCategory, position == 1)
        animateIndicator(indicatorAnalytics, position == 2)
        animateIndicator(indicatorSettings, position == 3)

        // Đổi màu icons và texts với animation mượt mà
        updateTabAppearance(iconHome, textHome, position == 0)
        updateTabAppearance(iconCategory, textCategory, position == 1)
        updateTabAppearance(iconAnalytics, textAnalytics, position == 2)
        updateTabAppearance(iconSettings, textSettings, position == 3)

        // Load fragment và hiện/ẩn nút Add
        when (position) {
            0 -> {
                loadFragment(HomeFragment())
//                btnAdd.show() // Hiện nút Add ở Home
            }
            1 -> {
                loadFragment(CategoryFragment())
//                btnAdd.hide() // Ẩn nút Add
            }
            2 -> {
                loadFragment(AnalyticsFragment())
//                btnAdd.hide() // Ẩn nút Add
            }
            3 -> {
                loadFragment(SettingsFragment())
//                btnAdd.hide() // Ẩn nút Add
            }
        }
    }

    private fun animateIndicator(indicator: View, show: Boolean) {
        if (show) {
            // Hiện indicator với animation mượt mà
            indicator.visibility = View.VISIBLE
            indicator.alpha = 0f
            indicator.scaleX = 0.5f
            indicator.animate()
                .alpha(1f)
                .scaleX(1f)
                .setDuration(300)
                .setInterpolator(OvershootInterpolator())
                .start()
        } else {
            // Ẩn indicator
            indicator.animate()
                .alpha(0f)
                .scaleX(0.5f)
                .setDuration(200)
                .withEndAction { indicator.visibility = View.INVISIBLE }
                .start()
        }
    }

    private fun updateTabAppearance(icon: ImageView, text: TextView, isSelected: Boolean) {
        val color = if (isSelected) {
            ContextCompat.getColor(this, R.color.primary_blue) // #2196F3 - Xanh da trời
        } else {
            ContextCompat.getColor(this, R.color.gray) // #9E9E9E - Xám
        }

        // Animate màu icon và text từ màu cũ sang màu mới
        ValueAnimator.ofArgb(
            icon.imageTintList?.defaultColor ?: color,
            color
        ).apply {
            duration = 300
            addUpdateListener { animator ->
                val animatedColor = animator.animatedValue as Int
                icon.imageTintList = ColorStateList.valueOf(animatedColor)
                text.setTextColor(animatedColor)
            }
            start()
        }

        // In đậm text khi được chọn
        text.typeface = if (isSelected) {
            Typeface.DEFAULT_BOLD
        } else {
            Typeface.DEFAULT
        }

        // Scale animation cho icon - phóng to nhẹ khi được chọn
        icon.animate()
            .scaleX(if (isSelected) 1.1f else 1f)
            .scaleY(if (isSelected) 1.1f else 1f)
            .setDuration(200)
            .start()
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}