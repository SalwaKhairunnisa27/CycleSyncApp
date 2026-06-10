package com.example.cyclesyncapp.ui.auth

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.cyclesyncapp.R

import android.content.Intent
import android.text.Html
import android.widget.Button
import android.widget.TextView
import com.example.cyclesyncapp.ui.dashboard.DashboardActivity

import com.example.cyclesyncapp.data.local.database.CycleDatabase
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.widget.EditText
import android.widget.Toast

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val database = CycleDatabase.getDatabase(this)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvSignUpLink = findViewById<TextView>(R.id.tvSignUpLink)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)

        // Styling "Belum punya akun? Daftar Sekarang"
        val signUpText = "Belum punya akun? <font color='#D4607A'><u>Daftar Sekarang</u></font>"
        tvSignUpLink.text = Html.fromHtml(signUpText, Html.FROM_HTML_MODE_LEGACY)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim().lowercase()
            val pass = etPassword.text.toString().trim()

            lifecycleScope.launch {
                val user = database.userDao().getUserByEmail(email)
                if (user != null && user.password == pass) {
                    // Save active user email in SharedPreferences
                    val prefs = getSharedPreferences("cycle_sync_prefs", MODE_PRIVATE)
                    prefs.edit().putString("active_user_email", email).apply()

                    startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Email atau password salah", Toast.LENGTH_SHORT).show()
                }
            }
        }

        tvSignUpLink.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }
    }
}