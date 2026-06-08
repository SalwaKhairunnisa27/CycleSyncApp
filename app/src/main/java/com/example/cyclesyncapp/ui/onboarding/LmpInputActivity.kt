package com.example.cyclesyncapp.ui.onboarding

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.cyclesyncapp.R
import java.text.SimpleDateFormat
import java.util.*

class LmpInputActivity : AppCompatActivity() {
    private var selectedLmpDate: String = ""
    private var selectedDuration: Int = 5 // default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lmp_input)

        val btnHitung = findViewById<Button>(R.id.btnHitung)
        val cardCalendar = findViewById<CardView>(R.id.cardLmpCalendar)
        val tvLmpSelected = findViewById<TextView>(R.id.tvLmpSelected)

        // Setup DatePicker for LMP
        cardCalendar.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val chosenCal = Calendar.getInstance()
                    chosenCal.set(year, month, dayOfMonth)
                    
                    val sdfDisplay = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                    tvLmpSelected.text = "Terpilih: " + sdfDisplay.format(chosenCal.time)
                    
                    val sdfSave = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    selectedLmpDate = sdfSave.format(chosenCal.time)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnHitung.setOnClickListener {
            if (selectedLmpDate.isEmpty()) {
                val cal = Calendar.getInstance()
                val sdfSave = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                selectedLmpDate = sdfSave.format(cal.time) // Fallback to today
            }

            // Save to SharedPreferences
            val prefs = getSharedPreferences("cyclesync_prefs", MODE_PRIVATE)
            prefs.edit().apply {
                putString("last_period", selectedLmpDate)
                putInt("period_duration", selectedDuration)
                apply()
            }

            val intent = Intent(this, CalculatingActivity::class.java)
            startActivity(intent)
        }

        // Setup click untuk durasi haid
        val durationButtons = listOf(
            Pair(findViewById<TextView>(R.id.btn3hr), 3),
            Pair(findViewById<TextView>(R.id.btn4hr), 4),
            Pair(findViewById<TextView>(R.id.btn5hr), 5),
            Pair(findViewById<TextView>(R.id.btn6hr), 6),
            Pair(findViewById<TextView>(R.id.btn7hr), 7)
        )

        durationButtons.forEach { pair ->
            val btn = pair.first
            val durValue = pair.second
            btn?.setOnClickListener {
                selectedDuration = durValue
                durationButtons.forEach { it.first?.setTextColor(resources.getColor(R.color.primary_text)) }
                btn.setTextColor(resources.getColor(R.color.primary))
            }
        }
    }
}
