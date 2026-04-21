package com.example.cyclesyncapp.ui.dashboard

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.cyclesyncapp.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class DailyLogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_log)

        setupMoodSelectors()
        setupEnergySelectors()
        setupGejalaSelectors()
        setupStatusHaidSelectors()
        
        findViewById<View>(R.id.btnSaveLog).setOnClickListener {
            finish()
        }
    }

    private fun setupMoodSelectors() {
        val moods = listOf(
            Pair(findViewById<LinearLayout>(R.id.moodBahagia), findViewById<View>(R.id.bgMoodBahagia)),
            Pair(findViewById<LinearLayout>(R.id.moodTenang), findViewById<View>(R.id.bgMoodTenang)),
            Pair(findViewById<LinearLayout>(R.id.moodSedih), findViewById<View>(R.id.bgMoodSedih)),
            Pair(findViewById<LinearLayout>(R.id.moodEmosi), findViewById<View>(R.id.bgMoodEmosi)),
            Pair(findViewById<LinearLayout>(R.id.moodLelah), findViewById<View>(R.id.bgMoodLelah))
        )

        moods.forEach { (layout, bg) ->
            layout.setOnClickListener {
                moods.forEach { it.second.setBackgroundResource(R.drawable.bg_mood_unselected) }
                bg.setBackgroundResource(R.drawable.bg_mood_selected)
            }
        }
    }

    private fun setupEnergySelectors() {
        val energyViews = listOf(
            findViewById<View>(R.id.energy1),
            findViewById<View>(R.id.energy2),
            findViewById<View>(R.id.energy3),
            findViewById<View>(R.id.energy4),
            findViewById<View>(R.id.energy5)
        )
        val tvScore = findViewById<TextView>(R.id.tvEnergyScore)

        energyViews.forEachIndexed { index, view ->
            view.setOnClickListener {
                val score = index + 1
                tvScore.text = "$score / 5"
                tvScore.setTextColor(ContextCompat.getColor(this, R.color.primary))
                energyViews.forEachIndexed { i, v ->
                    v.setBackgroundColor(
                        if (i <= index) ContextCompat.getColor(this, R.color.primary)
                        else ContextCompat.getColor(this, R.color.primary_soft)
                    )
                }
            }
        }
    }

    private fun setupGejalaSelectors() {
        val chipGroup = findViewById<ChipGroup>(R.id.chipGroupGejala)
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            chip?.setOnClickListener {
                val isSelected = chip.tag as? Boolean ?: false
                if (!isSelected) {
                    chip.setChipBackgroundColorResource(R.color.primary)
                    chip.setTextColor(ContextCompat.getColor(this, R.color.white))
                    chip.chipStrokeWidth = 0f
                    chip.tag = true
                } else {
                    chip.setChipBackgroundColorResource(R.color.white)
                    chip.setTextColor(ContextCompat.getColor(this, R.color.primary_muted))
                    chip.chipStrokeWidth = 1f
                    chip.setChipStrokeColorResource(R.color.primary_soft)
                    chip.tag = false
                }
            }
        }
    }

    private fun setupStatusHaidSelectors() {
        val statusViews = listOf(
            findViewById<TextView>(R.id.statusTidak),
            findViewById<TextView>(R.id.statusRingan),
            findViewById<TextView>(R.id.statusSedang),
            findViewById<TextView>(R.id.statusDeras)
        )

        statusViews.forEach { tv ->
            tv.setOnClickListener {
                statusViews.forEach {
                    it.setBackgroundResource(R.drawable.bg_card)
                    it.setTextColor(ContextCompat.getColor(this, R.color.primary_muted))
                }
                tv.setBackgroundResource(R.drawable.bg_pill_pink)
                tv.setTextColor(ContextCompat.getColor(this, R.color.primary))
            }
        }
    }
}
