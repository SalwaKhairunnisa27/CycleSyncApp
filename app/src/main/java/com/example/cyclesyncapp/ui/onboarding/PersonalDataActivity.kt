package com.example.cyclesyncapp.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.cyclesyncapp.R

class PersonalDataActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_data)

        val btnNext = findViewById<Button>(R.id.btnNext)
        val etNickname = findViewById<EditText>(R.id.etNickname)

        btnNext.setOnClickListener {
            val nickname = etNickname.text.toString()
            val intent = Intent(this, GoalSelectionActivity::class.java)
            intent.putExtra("nickname", nickname)
            startActivity(intent)
        }
    }
}