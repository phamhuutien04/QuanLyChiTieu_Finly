package com.example.quanlychitieu_finly

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.widget .TextView
import android.content.Intent

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        val txtGoLogin = findViewById<TextView>(R.id.txtGoLogin)

        txtGoLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // nếu muốn đóng màn hình đăng ký khi chuyển qua login
        }
    }
}