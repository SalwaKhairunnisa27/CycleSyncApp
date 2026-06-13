package com.example.cyclesyncapp.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.example.cyclesyncapp.R
import com.example.cyclesyncapp.data.local.database.CycleDatabase
import com.example.cyclesyncapp.data.repository.UserRepositoryImpl
import com.example.cyclesyncapp.ui.onboarding.LmpInputActivity
import kotlinx.coroutines.launch

class PostpartumDashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_postpartum_dashboard)

        findViewById<CardView>(R.id.btnInputLmp).setOnClickListener {
            startActivity(Intent(this, LmpInputActivity::class.java))
        }

        loadUserData()
    }

    private fun loadUserData() {
        val database = CycleDatabase.getDatabase(this)
        val userRepository = UserRepositoryImpl(database.userDao())
        
        lifecycleScope.launch {
            val prefs = getSharedPreferences("cycle_sync_prefs", MODE_PRIVATE)
            val activeEmail = prefs.getString("active_user_email", "aisyah@email.com") ?: "aisyah@email.com"
            val user = userRepository.getUserByEmail(activeEmail) ?: userRepository.getUser()
            user?.let {
                findViewById<TextView>(R.id.tvUserName).text = "${it.name ?: "Aisyah"} 🌸"
            }
        }
    }
}