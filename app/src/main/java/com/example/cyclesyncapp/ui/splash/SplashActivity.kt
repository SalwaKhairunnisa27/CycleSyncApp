package com.example.cyclesyncapp.ui.splash

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.cyclesyncapp.MainActivity
import com.example.cyclesyncapp.R

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        findViewById<Button>(R.id.btnGetStarted).setOnClickListener {
            // Kita arahkan langsung ke MainActivity agar bisa cek UI & Logic
            startActivity(Intent(this, MainActivity::class.java))
        }

        findViewById<TextView>(R.id.tvLoginLink).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}