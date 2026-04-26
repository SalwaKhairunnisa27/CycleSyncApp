package com.example.cyclesyncapp.ui.dashboard

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.cyclesyncapp.R
import com.example.cyclesyncapp.data.local.database.CycleDatabase
import com.example.cyclesyncapp.data.local.entity.DailyLogEntity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DailyLogActivity : AppCompatActivity() {

    private lateinit var database: CycleDatabase
    private var selectedMood: String = ""
    private var selectedEnergy: Int = 0
    private var selectedStatusHaid: String = ""
    private val selectedSymptoms = mutableSetOf<String>() // Pake Set biar gak dobel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_log)

        database = CycleDatabase.getDatabase(this)

        setupMoodSelectors()
        setupEnergySelectors()
        setupGejalaSelectors() // Ini yang kita perbaiki
        setupStatusHaidSelectors()

        findViewById<Button>(R.id.btnSaveLog).setOnClickListener {
            saveDailyLog()
        }
    }

    private fun setupGejalaSelectors() {
        val chipGroup = findViewById<ChipGroup>(R.id.chipGroupGejala)
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            chip?.apply {
                isCheckable = true // Paksa chip bisa dicentang
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedSymptoms.add(text.toString())
                        setChipBackgroundColorResource(R.color.primary)
                        setTextColor(ContextCompat.getColor(context, R.color.white))
                    } else {
                        selectedSymptoms.remove(text.toString())
                        setChipBackgroundColorResource(R.color.white)
                        setTextColor(ContextCompat.getColor(context, R.color.primary_muted))
                    }
                }
            }
        }
    }

    private fun saveDailyLog() {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val symptomsString = selectedSymptoms.joinToString(", ")

        val newLog = DailyLogEntity(
            date = currentDate,
            mood = selectedMood,
            symptoms = symptomsString,
            notes = "Energi: $selectedEnergy/5, Status: $selectedStatusHaid"
        )

        lifecycleScope.launch {
            try {
                database.dailyLogDao().insertLog(newLog)
                Toast.makeText(this@DailyLogActivity, "Log Berhasil Disimpan!", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Log.e("DATABASE_ERROR", "Gagal simpan log: ${e.message}")
            }
        }
    }

    // Kode setupMood, setupEnergy, setupStatusHaid tetap sama seperti sebelumnya...
    private fun setupMoodSelectors() {
        val moods = listOf(
            Triple(findViewById<LinearLayout>(R.id.moodBahagia), findViewById<View>(R.id.bgMoodBahagia), "Bahagia"),
            Triple(findViewById<LinearLayout>(R.id.moodTenang), findViewById<View>(R.id.bgMoodTenang), "Tenang"),
            Triple(findViewById<LinearLayout>(R.id.moodSedih), findViewById<View>(R.id.bgMoodSedih), "Sedih"),
            Triple(findViewById<LinearLayout>(R.id.moodEmosi), findViewById<View>(R.id.bgMoodEmosi), "Emosional"),
            Triple(findViewById<LinearLayout>(R.id.moodLelah), findViewById<View>(R.id.bgMoodLelah), "Lelah")
        )
        moods.forEach { (layout, bg, name) ->
            layout.setOnClickListener {
                moods.forEach { it.second.setBackgroundResource(R.drawable.bg_mood_unselected) }
                bg.setBackgroundResource(R.drawable.bg_mood_selected)
                selectedMood = name
            }
        }
    }

    private fun setupEnergySelectors() {
        val energyViews = listOf(findViewById<View>(R.id.energy1), findViewById<View>(R.id.energy2), findViewById<View>(R.id.energy3), findViewById<View>(R.id.energy4), findViewById<View>(R.id.energy5))
        val tvScore = findViewById<TextView>(R.id.tvEnergyScore)
        energyViews.forEachIndexed { index, view ->
            view.setOnClickListener {
                selectedEnergy = index + 1
                tvScore.text = "$selectedEnergy / 5"
                energyViews.forEachIndexed { i, v -> v.setBackgroundColor(if (i <= index) ContextCompat.getColor(this, R.color.primary) else ContextCompat.getColor(this, R.color.primary_soft)) }
            }
        }
    }

    private fun setupStatusHaidSelectors() {
        val statusList = listOf(Pair(findViewById<TextView>(R.id.statusTidak), "Tidak Ada"), Pair(findViewById<TextView>(R.id.statusRingan), "Ringan"), Pair(findViewById<TextView>(R.id.statusSedang), "Sedang"), Pair(findViewById<TextView>(R.id.statusDeras), "Deras"))
        statusList.forEach { (tv, statusName) ->
            tv.setOnClickListener {
                statusList.forEach { it.first.setBackgroundResource(R.drawable.bg_card); it.first.setTextColor(ContextCompat.getColor(this, R.color.primary_muted)) }
                tv.setBackgroundResource(R.drawable.bg_pill_pink); tv.setTextColor(ContextCompat.getColor(this, R.color.primary))
                selectedStatusHaid = statusName
            }
        }
    }
}