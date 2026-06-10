package com.example.cyclesyncapp.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.cyclesyncapp.R
import com.example.cyclesyncapp.ui.onboarding.LmpInputActivity

class PostpartumDashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_postpartum_dashboard)

        findViewById<CardView>(R.id.btnInputLmp).setOnClickListener {
            startActivity(Intent(this, LmpInputActivity::class.java))
        }
    }
}