package com.example.quanlychitieu_finly

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton

class SettingsFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences

    // Views
    private lateinit var tvCurrentTheme: TextView
    private lateinit var tvCurrentLanguage: TextView
    private lateinit var tvCurrentCurrency: TextView

    private lateinit var switch2FA: SwitchCompat
    private lateinit var switchAutoSync: SwitchCompat
    private lateinit var switchBudgetAlert: SwitchCompat
    private lateinit var switchPushNotif: SwitchCompat

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        sharedPreferences = requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        initViews(view)
        loadSettings()
        setupClickListeners(view)

        return view
    }

    private fun initViews(view: View) {
        tvCurrentTheme = view.findViewById(R.id.tvCurrentTheme)
        tvCurrentLanguage = view.findViewById(R.id.tvCurrentLanguage)
        tvCurrentCurrency = view.findViewById(R.id.tvCurrentCurrency)

        switch2FA = view.findViewById(R.id.switch2FA)
        switchAutoSync = view.findViewById(R.id.switchAutoSync)
        switchBudgetAlert = view.findViewById(R.id.switchBudgetAlert)
        switchPushNotif = view.findViewById(R.id.switchPushNotif)
    }

    private fun loadSettings() {
        // Load saved settings
        tvCurrentTheme.text = sharedPreferences.getString("theme_mode", "Tự động (Theo hệ thống)")
        tvCurrentLanguage.text = sharedPreferences.getString("language", "Tiếng Việt")
        tvCurrentCurrency.text = sharedPreferences.getString("currency", "VND (₫)")

        switch2FA.isChecked = sharedPreferences.getBoolean("2fa_enabled", false)
        switchAutoSync.isChecked = sharedPreferences.getBoolean("auto_sync", true)
        switchBudgetAlert.isChecked = sharedPreferences.getBoolean("budget_alert", true)
        switchPushNotif.isChecked = sharedPreferences.getBoolean("push_notif", true)
    }

    private fun setupClickListeners(view: View) {
        // User Profile
        view.findViewById<CardView>(R.id.cardUserProfile).setOnClickListener {
            Toast.makeText(requireContext(), "Chỉnh sửa hồ sơ", Toast.LENGTH_SHORT).show()
        }

        // Change Password
        view.findViewById<LinearLayout>(R.id.btnChangePassword).setOnClickListener {
            showChangePasswordDialog()
        }

        // Social Login
        view.findViewById<LinearLayout>(R.id.btnSocialLogin).setOnClickListener {
            Toast.makeText(requireContext(), "Quản lý đăng nhập mạng xã hội", Toast.LENGTH_SHORT).show()
        }

        // 2FA Switch
        switch2FA.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("2fa_enabled", isChecked).apply()
            if (isChecked) {
                show2FASetupDialog()
            }
        }

        // Linked Wallets
        view.findViewById<LinearLayout>(R.id.btnLinkedWallets).setOnClickListener {
            Toast.makeText(requireContext(), "Quản lý ví liên kết", Toast.LENGTH_SHORT).show()
        }

        // Auto Sync Switch
        switchAutoSync.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("auto_sync", isChecked).apply()
            Toast.makeText(
                requireContext(),
                if (isChecked) "Đã bật đồng bộ tự động" else "Đã tắt đồng bộ tự động",
                Toast.LENGTH_SHORT
            ).show()
        }

        // OCR Settings
        view.findViewById<LinearLayout>(R.id.btnOCRSettings).setOnClickListener {
            Toast.makeText(requireContext(), "Cài đặt quét hóa đơn", Toast.LENGTH_SHORT).show()
        }

        // Budget Alert Switch
        switchBudgetAlert.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("budget_alert", isChecked).apply()
        }

        // Push Notification Switch
        switchPushNotif.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("push_notif", isChecked).apply()
        }

        // Sound Settings
        view.findViewById<LinearLayout>(R.id.btnSoundSettings).setOnClickListener {
            Toast.makeText(requireContext(), "Cài đặt âm thanh", Toast.LENGTH_SHORT).show()
        }

        // Theme Mode
        view.findViewById<LinearLayout>(R.id.btnThemeMode).setOnClickListener {
            showThemeModeDialog()
        }

        // Accent Color
        view.findViewById<LinearLayout>(R.id.btnAccentColor).setOnClickListener {
            showAccentColorBottomSheet()
        }

        // Language
        view.findViewById<LinearLayout>(R.id.btnLanguage).setOnClickListener {
            showLanguageDialog()
        }

        // Currency
        view.findViewById<LinearLayout>(R.id.btnCurrency).setOnClickListener {
            showCurrencyDialog()
        }

        // Privacy Policy
        view.findViewById<LinearLayout>(R.id.btnPrivacyPolicy).setOnClickListener {
            Toast.makeText(requireContext(), "Chính sách & Điều khoản", Toast.LENGTH_SHORT).show()
        }

        // Help & Support
        view.findViewById<LinearLayout>(R.id.btnHelpSupport).setOnClickListener {
            Toast.makeText(requireContext(), "Trợ giúp & Hỗ trợ", Toast.LENGTH_SHORT).show()
        }

        // Feedback
        view.findViewById<LinearLayout>(R.id.btnFeedback).setOnClickListener {
            Toast.makeText(requireContext(), "Gửi phản hồi", Toast.LENGTH_SHORT).show()
        }

        // About
        view.findViewById<LinearLayout>(R.id.btnAbout).setOnClickListener {
            showAboutDialog()
        }

        // Logout
        view.findViewById<CardView>(R.id.btnLogout).setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun Dialog.applyFullWidthWithMargin(marginDp: Int = 20) {
        val metrics = resources.displayMetrics
        val marginPx = (marginDp * metrics.density).toInt()
        val width = metrics.widthPixels - (marginPx * 2)

        window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    // ==================== THEME MODE DIALOG ====================
    private fun showThemeModeDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_theme_mode)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // ======== PHẦN SET WIDTH FULL ========

        dialog.show()
        dialog.applyFullWidthWithMargin(20)

        // =====================================

        val cardLight = dialog.findViewById<CardView>(R.id.cardLightMode)
        val cardDark = dialog.findViewById<CardView>(R.id.cardDarkMode)
        val cardAuto = dialog.findViewById<CardView>(R.id.cardAutoMode)

        val rbLight = dialog.findViewById<RadioButton>(R.id.rbLightMode)
        val rbDark = dialog.findViewById<RadioButton>(R.id.rbDarkMode)
        val rbAuto = dialog.findViewById<RadioButton>(R.id.rbAutoMode)

        // Load current selection
        val currentTheme = sharedPreferences.getString("theme_mode", "Tự động (Theo hệ thống)")
        when (currentTheme) {
            "Sáng" -> rbLight.isChecked = true
            "Tối" -> rbDark.isChecked = true
            else -> rbAuto.isChecked = true
        }

        // Card click listeners
        cardLight.setOnClickListener {
            rbLight.isChecked = true
            rbDark.isChecked = false
            rbAuto.isChecked = false
        }

        cardDark.setOnClickListener {
            rbDark.isChecked = true
            rbLight.isChecked = false
            rbAuto.isChecked = false
        }

        cardAuto.setOnClickListener {
            rbAuto.isChecked = true
            rbLight.isChecked = false
            rbDark.isChecked = false
        }

        dialog.findViewById<TextView>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<TextView>(R.id.btnApply).setOnClickListener {
            val selectedTheme = when {
                rbLight.isChecked -> "Sáng"
                rbDark.isChecked -> "Tối"
                else -> "Tự động (Theo hệ thống)"
            }

            sharedPreferences.edit().putString("theme_mode", selectedTheme).apply()
            tvCurrentTheme.text = selectedTheme
            Toast.makeText(requireContext(), "Đã áp dụng chế độ $selectedTheme", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    // ==================== ACCENT COLOR BOTTOM SHEET ====================
    private fun showAccentColorBottomSheet() {
        val bottomSheet = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_accent_color, null)
        bottomSheet.setContentView(view)

        val colors = mapOf(
            R.id.colorOrange to Pair("#F59E0B", "Cam"),
            R.id.colorGreen to Pair("#10B981", "Xanh lá"),
            R.id.colorBlue to Pair("#3B82F6", "Xanh dương"),
            R.id.colorPurple to Pair("#8B5CF6", "Tím"),
            R.id.colorRed to Pair("#EF4444", "Đỏ"),
            R.id.colorPink to Pair("#EC4899", "Hồng"),
            R.id.colorTeal to Pair("#14B8A6", "Xanh ngọc"),
            R.id.colorIndigo to Pair("#6366F1", "Chàm")
        )

        var selectedColor = "Xanh lá" // Default

        colors.forEach { (layoutId, colorInfo) ->
            view.findViewById<LinearLayout>(layoutId).setOnClickListener {
                // Hide all checkmarks
                colors.keys.forEach { id ->
                    val checkId = when (id) {
                        R.id.colorOrange -> R.id.checkOrange
                        R.id.colorGreen -> R.id.checkGreen
                        R.id.colorBlue -> R.id.checkBlue
                        R.id.colorPurple -> R.id.checkPurple
                        R.id.colorRed -> R.id.checkRed
                        R.id.colorPink -> R.id.checkPink
                        R.id.colorTeal -> R.id.checkTeal
                        R.id.colorIndigo -> R.id.checkIndigo
                        else -> 0
                    }
                    if (checkId != 0) {
                        view.findViewById<ImageView>(checkId).visibility = View.GONE
                    }
                }

                // Show selected checkmark
                val checkId = when (layoutId) {
                    R.id.colorOrange -> R.id.checkOrange
                    R.id.colorGreen -> R.id.checkGreen
                    R.id.colorBlue -> R.id.checkBlue
                    R.id.colorPurple -> R.id.checkPurple
                    R.id.colorRed -> R.id.checkRed
                    R.id.colorPink -> R.id.checkPink
                    R.id.colorTeal -> R.id.checkTeal
                    R.id.colorIndigo -> R.id.checkIndigo
                    else -> 0
                }
                if (checkId != 0) {
                    view.findViewById<ImageView>(checkId).visibility = View.VISIBLE
                }

                selectedColor = colorInfo.second
            }
        }

        view.findViewById<MaterialButton>(R.id.btnApplyColor).setOnClickListener {
            sharedPreferences.edit().putString("accent_color", selectedColor).apply()
            Toast.makeText(requireContext(), "Đã áp dụng màu $selectedColor", Toast.LENGTH_SHORT).show()
            bottomSheet.dismiss()
        }

        bottomSheet.show()
    }

    // ==================== LANGUAGE DIALOG ====================
    private fun showLanguageDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_language)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val rbVietnamese = dialog.findViewById<RadioButton>(R.id.rbVietnamese)
        val rbEnglish = dialog.findViewById<RadioButton>(R.id.rbEnglish)

        // Load current selection
        val currentLang = sharedPreferences.getString("language", "Tiếng Việt")
        when (currentLang) {
            "Tiếng Việt" -> rbVietnamese.isChecked = true
            "English" -> rbEnglish.isChecked = true
        }

        dialog.findViewById<TextView>(R.id.btnCancelLang).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<TextView>(R.id.btnApplyLang).setOnClickListener {
            val selectedLang = if (rbVietnamese.isChecked) "Tiếng Việt" else "English"

            sharedPreferences.edit().putString("language", selectedLang).apply()
            tvCurrentLanguage.text = selectedLang
            Toast.makeText(requireContext(), "Language changed to $selectedLang", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
        dialog.applyFullWidthWithMargin(20)
    }

    // ==================== CURRENCY DIALOG ====================
    private fun showCurrencyDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_currency)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val rbVND = dialog.findViewById<RadioButton>(R.id.rbVND)
        val rbUSD = dialog.findViewById<RadioButton>(R.id.rbUSD)
        val rbEUR = dialog.findViewById<RadioButton>(R.id.rbEUR)
        val rbTHB = dialog.findViewById<RadioButton>(R.id.rbTHB)

        // Load current selection
        val currentCurrency = sharedPreferences.getString("currency", "VND (₫)")
        when (currentCurrency) {
            "VND (₫)" -> rbVND.isChecked = true
            "USD ($)" -> rbUSD.isChecked = true
            "EUR (€)" -> rbEUR.isChecked = true
            "THB (฿)" -> rbTHB.isChecked = true
        }

        dialog.findViewById<TextView>(R.id.btnCancelCurrency).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<TextView>(R.id.btnApplyCurrency).setOnClickListener {
            val selectedCurrency = when {
                rbVND.isChecked -> "VND (₫)"
                rbUSD.isChecked -> "USD ($)"
                rbEUR.isChecked -> "EUR (€)"
                rbTHB.isChecked -> "THB (฿)"
                else -> "VND (₫)"
            }

            sharedPreferences.edit().putString("currency", selectedCurrency).apply()
            tvCurrentCurrency.text = selectedCurrency
            Toast.makeText(requireContext(), "Đã đổi sang $selectedCurrency", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
        dialog.applyFullWidthWithMargin(20)
    }

    // ==================== CHANGE PASSWORD DIALOG ====================
    private fun showChangePasswordDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_change_password)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val etOldPassword = dialog.findViewById<EditText>(R.id.etOldPassword)
        val etNewPassword = dialog.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = dialog.findViewById<EditText>(R.id.etConfirmPassword)

        dialog.findViewById<TextView>(R.id.btnCancelPassword).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<TextView>(R.id.btnSavePassword).setOnClickListener {
            val oldPass = etOldPassword.text.toString()
            val newPass = etNewPassword.text.toString()
            val confirmPass = etConfirmPassword.text.toString()

            when {
                oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty() -> {
                    Toast.makeText(requireContext(), "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                }
                newPass != confirmPass -> {
                    Toast.makeText(requireContext(), "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show()
                }
                newPass.length < 6 -> {
                    Toast.makeText(requireContext(), "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // TODO: Call API to change password
                    Toast.makeText(requireContext(), "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
        dialog.applyFullWidthWithMargin(20)
    }

    // ==================== 2FA SETUP DIALOG ====================
    private fun show2FASetupDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_2fa_setup)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.findViewById<MaterialButton>(R.id.btnEnable2FA).setOnClickListener {
            Toast.makeText(requireContext(), "2FA đã được kích hoạt", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.findViewById<TextView>(R.id.btnSkip2FA).setOnClickListener {
            switch2FA.isChecked = false
            dialog.dismiss()
        }

        dialog.show()
        dialog.applyFullWidthWithMargin(20)
    }

    // ==================== ABOUT DIALOG ====================
    private fun showAboutDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_about)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.findViewById<MaterialButton>(R.id.btnCloseAbout).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.applyFullWidthWithMargin(20)
    }

    // ==================== LOGOUT DIALOG ====================
    private fun showLogoutDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_logout)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.findViewById<TextView>(R.id.btnCancelLogout).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<MaterialButton>(R.id.btnConfirmLogout).setOnClickListener {
            // TODO: Clear session and navigate to login
            Toast.makeText(requireContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
        dialog.applyFullWidthWithMargin(20)
    }
}