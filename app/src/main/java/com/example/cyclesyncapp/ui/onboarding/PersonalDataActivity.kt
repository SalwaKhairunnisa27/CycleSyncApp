package com.example.cyclesyncapp.ui.onboarding

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.cyclesyncapp.R
import java.util.Calendar

class PersonalDataActivity : AppCompatActivity() {
    private var selectedDob: String = ""
    private var calculatedAge: Int = 25 // default fallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_data)

        val btnNext = findViewById<Button>(R.id.btnNext)
        val etNickname = findViewById<EditText>(R.id.etNickname)
        val tvDob = findViewById<TextView>(R.id.tvDob)
        val etHeight = findViewById<EditText>(R.id.etHeight)
        val etWeight = findViewById<EditText>(R.id.etWeight)

        // DOB Click Listener
        tvDob.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedDob = "$dayOfMonth/${month + 1}/$year"
                    tvDob.text = selectedDob
                    
                    // Calculate age dynamically
                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                    calculatedAge = currentYear - year
                },
                cal.get(Calendar.YEAR) - 25, // default showing 25 years ago
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnNext.setOnClickListener {
            val nickname = etNickname.text.toString()
            val heightStr = etHeight.text.toString()
            val weightStr = etWeight.text.toString()

            val height = heightStr.toDoubleOrNull() ?: 160.0
            val weight = weightStr.toDoubleOrNull() ?: 55.0

            // Save to SharedPreferences
            val prefs = getSharedPreferences("cyclesync_prefs", MODE_PRIVATE)
            prefs.edit().apply {
                putString("nickname", nickname)
                putInt("age", calculatedAge)
                putFloat("height", height.toFloat())
                putFloat("weight", weight.toFloat())
                apply()
            }

            val intent = Intent(this, GoalSelectionActivity::class.java)
            intent.putExtra("nickname", nickname)
            startActivity(intent)
        }
    }
}