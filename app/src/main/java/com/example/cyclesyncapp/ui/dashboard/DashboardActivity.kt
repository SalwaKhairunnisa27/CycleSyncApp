package com.example.cyclesyncapp.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cyclesyncapp.R
import com.example.cyclesyncapp.data.local.database.CycleDatabase
import com.example.cyclesyncapp.ui.calendar.CalendarActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    // Deklarasi database
    private lateinit var database: CycleDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // 1. Inisialisasi Database
        database = CycleDatabase.getDatabase(this)

        // 2. Tes apakah data "articles" udah masuk (Cek di Logcat Android Studio)
        lifecycleScope.launch {
            try {
                // Tambahkan .first() setelah panggil Dao
                val articles = database.educationDao().getAllArticles().first()

                Log.d("DATABASE_CHECK", "JUMLAH ARTIKEL DITEMUKAN: ${articles.size}")

                if (articles.isNotEmpty()) {
                    Log.d("DATABASE_CHECK", "Judul artikel pertama: ${articles[0].title}")
                }
            } catch (e: Exception) {
                Log.e("DATABASE_CHECK", "Error panggil database: ${e.message}")
            }
        }

        findViewById<android.view.View>(R.id.cardLogHarian).setOnClickListener {
            startActivity(Intent(this, DailyLogActivity::class.java))
        }

        findViewById<android.view.View>(R.id.navSiklus).setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }

        findViewById<android.view.View>(R.id.navLog).setOnClickListener {
            startActivity(Intent(this, DailyLogActivity::class.java))
        }
    }
}