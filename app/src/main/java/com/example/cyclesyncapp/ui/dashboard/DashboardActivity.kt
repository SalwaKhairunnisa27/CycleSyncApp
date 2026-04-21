package com.example.cyclesyncapp.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.cyclesyncapp.R
import com.example.cyclesyncapp.ui.calendar.CalendarActivity

class DashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Quick card navigations
        findViewById<android.view.View>(R.id.cardLogHarian).setOnClickListener {
            startActivity(Intent(this, DailyLogActivity::class.java))
        }

        // Bottom nav
        findViewById<android.view.View>(R.id.navSiklus).setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }

        findViewById<android.view.View>(R.id.navLog).setOnClickListener {
            startActivity(Intent(this, DailyLogActivity::class.java))
        }
    }
}