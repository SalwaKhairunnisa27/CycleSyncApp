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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_data)

        val btnNext = findViewById<Button>(R.id.btnNext)
        val etNickname = findViewById<EditText>(R.id.etNickname)
        val tvDob = findViewById<TextView>(R.id.tvDob)

        tvDob.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                tvDob.text = "$selectedDay/${selectedMonth + 1}/$selectedYear"
            }, year, month, day)
            datePickerDialog.show()
        }

        btnNext.setOnClickListener {
            val nickname = etNickname.text.toString()
            if (nickname.isEmpty()) {
                etNickname.error = "Nama panggilan wajib diisi"
                return@setOnClickListener
            }
            val intent = Intent(this, GoalSelectionActivity::class.java)
            intent.putExtra("nickname", nickname)
            startActivity(intent)
        }
    }
}