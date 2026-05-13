package com.example.cyclesyncapp.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cyclesyncapp.R
import com.example.cyclesyncapp.data.local.preferences.UserPreferences
import java.util.Calendar

class LmpInputActivity : AppCompatActivity() {

    private var selectedYear: Int = -1
    private var selectedMonth: Int = -1
    private var selectedDay: Int = -1
    private var selectedCycleLength: Int = 28 // default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lmp_input)

        val nickname = intent.getStringExtra("nickname") ?: "User"
        val goal = intent.getStringExtra("goal") ?: "rutin"

        // Set default ke hari ini
        val today = Calendar.getInstance()
        selectedYear = today.get(Calendar.YEAR)
        selectedMonth = today.get(Calendar.MONTH)
        selectedDay = today.get(Calendar.DAY_OF_MONTH)

        // CalendarView — tangkap tanggal yang dipilih user
        val calendarLmp = findViewById<CalendarView>(R.id.calendarLmp)
        calendarLmp.maxDate = System.currentTimeMillis()
        calendarLmp.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedYear = year
            selectedMonth = month
            selectedDay = dayOfMonth
        }

        // Pilih panjang siklus
        val cycleBtns = mapOf(
            R.id.btn26 to 26,
            R.id.btn27 to 27,
            R.id.btn28 to 28,
            R.id.btn29 to 29,
            R.id.btn30 to 30
        )
        val allBtnIds = cycleBtns.keys.toList()

        cycleBtns.forEach { (btnId, cycleLen) ->
            findViewById<TextView>(btnId)?.setOnClickListener {
                selectedCycleLength = cycleLen
                // Reset semua
                allBtnIds.forEach { id ->
                    val v = findViewById<TextView>(id)
                    v?.setBackgroundResource(R.drawable.bg_card)
                    v?.setTextColor(resources.getColor(R.color.primary_text, null))
                    v?.text = "${resources.getInteger(android.R.integer.config_longAnimTime).let { cycleLen }}"
                }
                // Highlight yang dipilih
                val selected = findViewById<TextView>(btnId)
                selected?.setBackgroundResource(R.drawable.bg_pill_pink)
                selected?.setTextColor(resources.getColor(R.color.primary, null))
            }
        }
        // Tampilkan teks yang benar untuk tiap btn (ada yang mungkin tertimpa)
        cycleBtns.forEach { (btnId, cycleLen) ->
            val suffix = if (cycleLen == 28) " ✓" else ""
            findViewById<TextView>(btnId)?.text = "$cycleLen$suffix"
        }

        val btnHitung = findViewById<Button>(R.id.btnHitung)
        btnHitung.setOnClickListener {
            if (selectedYear == -1) {
                Toast.makeText(this, "Pilih tanggal haid terakhirmu dulu ya!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prefs = UserPreferences(this)
            prefs.saveNickname(nickname)
            prefs.saveGoal(goal)
            // ✅ Simpan LMP date yang nyata
            prefs.saveLmpDate(selectedYear, selectedMonth, selectedDay)
            // Simpan panjang siklus sebagai history
            // (kita gunakan selectedCycleLength sebagai nilai tunggal)
            prefs.saveNextPeriodDays(selectedCycleLength)

            val intent = Intent(this, CalculatingActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
