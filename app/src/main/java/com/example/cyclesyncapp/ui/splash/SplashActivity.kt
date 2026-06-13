package com.example.cyclesyncapp.ui.splash

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.cyclesyncapp.R
import com.example.cyclesyncapp.ui.dashboard.DashboardActivity // Import Dashboard
import com.example.cyclesyncapp.ui.setup.SetupActivity // Import Setup

import android.text.Html

import com.example.cyclesyncapp.data.local.database.CycleDatabase
import com.example.cyclesyncapp.data.local.entity.UserEntity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Auto-login check
        val prefs = getSharedPreferences("cycle_sync_prefs", MODE_PRIVATE)
        val activeEmail = prefs.getString("active_user_email", null)
        if (activeEmail != null) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_splash)

        val database = CycleDatabase.getDatabase(this)
        seedDummyUser(database)

        val tvLoginLink = findViewById<TextView>(R.id.tvLoginLink)
        
        // Styling "Sudah punya akun? Masuk"
        val loginText = "Sudah punya akun? <u>Masuk</u>"
        tvLoginLink.text = Html.fromHtml(loginText, Html.FROM_HTML_MODE_LEGACY)

        findViewById<Button>(R.id.btnGetStarted).setOnClickListener {
            startActivity(Intent(this, com.example.cyclesyncapp.ui.auth.SignUpActivity::class.java))
        }

        tvLoginLink.setOnClickListener {
            startActivity(Intent(this, com.example.cyclesyncapp.ui.auth.LoginActivity::class.java))
        }
    }

    private fun seedDummyUser(database: CycleDatabase) {
        lifecycleScope.launch {
            val user = database.userDao().getUserByEmail("aisyah@email.com")
            if (user == null) {
                val dummyUser = UserEntity(
                    name = "Aisyah ✨",
                    email = "aisyah@email.com",
                    password = "password123",
                    age = 25,
                    weight = 55.0,
                    height = 160.0,
                    cycleLength = 28,
                    isPregnant = false
                )
                database.userDao().insertUser(dummyUser)

                // Tambahkan data histori siklus (3 bulan terakhir)
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val cal = Calendar.getInstance()
                
                // Siklus 1 bulan lalu
                cal.add(Calendar.DAY_OF_YEAR, -28)
                val start1 = sdf.format(cal.time)
                
                // Seed onboarding_lmp in SharedPreferences
                val prefs = getSharedPreferences("cycle_sync_prefs", MODE_PRIVATE)
                prefs.edit().putString("onboarding_lmp", start1).apply()
                
                cal.add(Calendar.DAY_OF_YEAR, 5)
                val end1 = sdf.format(cal.time)
                database.cycleDao().insertCycle(com.example.cyclesyncapp.data.local.entity.CycleEntity(startDate = start1, endDate = end1, cycleLength = 28, periodLength = 5))

                // Tambahkan beberapa log harian untuk preview
                val today = sdf.format(Date())
                database.dailyLogDao().insertLog(com.example.cyclesyncapp.data.local.entity.DailyLogEntity(
                    date = today,
                    flowLevel = "Medium",
                    symptoms = "Kram Perut, Kembung",
                    encryptedNote = "Dummy note",
                    phase = "MENSTRUATION"
                ))
            }
        }
    }
}