package com.example.cyclesyncapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.cyclesyncapp.R
import com.example.cyclesyncapp.ui.onboarding.PersonalDataActivity
import com.example.cyclesyncapp.ui.dashboard.DashboardActivity

import com.example.cyclesyncapp.data.local.database.CycleDatabase
import com.example.cyclesyncapp.data.local.entity.UserEntity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.widget.EditText
import android.widget.Toast

class SignUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val database = CycleDatabase.getDatabase(this)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvLoginLink = findViewById<TextView>(R.id.tvLoginLink)
        val tvTerms = findViewById<TextView>(R.id.tvTerms)
        
        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)

        // Styling "Sudah punya akun? Masuk di sini"
        val loginText = "Sudah punya akun? <font color='#D4607A'><u>Masuk di sini</u></font>"
        tvLoginLink.text = Html.fromHtml(loginText, Html.FROM_HTML_MODE_LEGACY)

        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim().lowercase()
            val name = etName.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Email dan Password tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val existingUser = database.userDao().getUserByEmail(email)
                if (existingUser != null) {
                    Toast.makeText(this@SignUpActivity, "Email sudah terdaftar!", Toast.LENGTH_SHORT).show()
                } else {
                    val newUser = UserEntity(
                        name = name,
                        email = email,
                        password = pass,
                        age = 25,
                        weight = 55.0,
                        height = 160.0,
                        cycleLength = 28
                    )
                    database.userDao().insertUser(newUser)
                    
                    // Clear database tables for cycles and daily logs to give the new user a clean slate
                    database.cycleDao().clearCycles()
                    database.dailyLogDao().clearLogs()
                    
                    // Save active user email in SharedPreferences and reset onboarding steps
                    val prefs = getSharedPreferences("cycle_sync_prefs", MODE_PRIVATE)
                    prefs.edit().putString("active_user_email", email)
                        .putInt("onboarding_step", 0)
                        .putBoolean("is_first_launch", true)
                        .putBoolean("onboarding_tour_completed", false)
                        .apply()

                    // Put nickname in intent to pass to onboarding
                    val intent = Intent(this@SignUpActivity, PersonalDataActivity::class.java).apply {
                        putExtra("nickname", name)
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }

        tvLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
