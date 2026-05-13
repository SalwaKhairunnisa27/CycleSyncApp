package com.example.cyclesyncapp.ui.splash

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.cyclesyncapp.R
import com.example.cyclesyncapp.ui.onboarding.PersonalDataActivity
import com.example.cyclesyncapp.ui.dashboard.DashboardActivity
import com.example.cyclesyncapp.data.local.preferences.UserPreferences

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = UserPreferences(this)
        if (prefs.isOnboardingComplete()) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }
        
        setContentView(R.layout.activity_splash)

        findViewById<Button>(R.id.btnGetStarted).setOnClickListener {
            startActivity(Intent(this, PersonalDataActivity::class.java))
            finish()
        }

        findViewById<TextView>(R.id.tvLoginLink).setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }
    }
}