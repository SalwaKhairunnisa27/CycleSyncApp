package com.example.cyclesyncapp.ui.splash

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.cyclesyncapp.R
import com.example.cyclesyncapp.ui.auth.SignUpActivity
import com.example.cyclesyncapp.ui.dashboard.DashboardActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        findViewById<Button>(R.id.btnGetStarted).setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        findViewById<TextView>(R.id.tvLoginLink).setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }
    }
}