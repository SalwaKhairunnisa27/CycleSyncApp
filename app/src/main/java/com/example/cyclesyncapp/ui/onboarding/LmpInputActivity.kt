package com.example.cyclesyncapp.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.cyclesyncapp.R

class LmpInputActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lmp_input)

        // Tombol Hitung → langsung ke CalculatingActivity (Dummy Flow)
        val btnHitung = findViewById<Button>(R.id.btnHitung)
        
        btnHitung.setOnClickListener {
            // Karena ini masih fase UI, kita langsung lanjut saja tanpa validasi database
            val intent = Intent(this, CalculatingActivity::class.java)
            startActivity(intent)
        }

        // Setup dummy click untuk durasi haid agar tidak crash
        val durationButtons = listOf(
            findViewById<TextView>(R.id.btn3hr),
            findViewById<TextView>(R.id.btn4hr),
            findViewById<TextView>(R.id.btn5hr),
            findViewById<TextView>(R.id.btn6hr),
            findViewById<TextView>(R.id.btn7hr)
        )

        durationButtons.forEach { btn ->
            btn?.setOnClickListener {
                // Simulasi memilih durasi (visual only)
                durationButtons.forEach { it?.setTextColor(resources.getColor(R.color.primary_text)) }
                btn.setTextColor(resources.getColor(R.color.primary))
            }
        }
    }
}
