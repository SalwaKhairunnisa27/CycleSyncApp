package com.example.cyclesyncapp.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.cyclesyncapp.R
import com.example.cyclesyncapp.ui.dashboard.DashboardActivity
import com.example.cyclesyncapp.data.local.preferences.UserPreferences

class CalculatingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculating)

        // Animasi check item ke-3 setelah 1.5 detik, lalu ke dashboard setelah 2.5 detik
        Handler(Looper.getMainLooper()).postDelayed({
            findViewById<TextView>(R.id.tvCheckPersonalisasi).text =
                "✓ Menyesuaikan rekomendasi personal"
        }, 1500)

        Handler(Looper.getMainLooper()).postDelayed({
            val prefs = UserPreferences(this)
            prefs.saveOnboardingComplete(true)
            
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }, 2500)
    }
}