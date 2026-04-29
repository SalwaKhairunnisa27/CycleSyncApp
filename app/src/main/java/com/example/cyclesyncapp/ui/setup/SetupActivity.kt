package com.example.cyclesyncapp.ui.setup

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.example.cyclesyncapp.ui.activity.MainActivity
import com.example.cyclesyncapp.databinding.ActivitySetupBinding
import java.util.*

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private var selectedDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Date picker
        binding.btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                selectedDate = "$d/${m + 1}/$y"
                binding.tvSelectedDate.text = selectedDate
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Cycle length slider
        binding.seekCycleLength.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvCycleLength.text = "$progress days"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Period duration slider
        binding.seekPeriodDuration.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvPeriodDuration.text = "$progress days"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Save & go to main
        binding.btnStartJourney.setOnClickListener {
            val prefs = getSharedPreferences("cyclesync_prefs", MODE_PRIVATE)
            prefs.edit().apply {
                putBoolean("setup_done", true)
                putString("last_period", selectedDate)
                putInt("cycle_length", binding.seekCycleLength.progress)
                putInt("period_duration", binding.seekPeriodDuration.progress)
            }.apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}