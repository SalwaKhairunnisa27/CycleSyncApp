package com.example.cyclesyncapp.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.cyclesyncapp.R

class GoalSelectionActivity : AppCompatActivity() {

    private var selectedGoal = "rutin" // default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal_selection)

        val cardRutin = findViewById<CardView>(R.id.cardGoalTrack)
        val cardPromil = findViewById<CardView>(R.id.cardGoalConceive)
        val btnLanjut = findViewById<Button>(R.id.btnNext)
        val btnBack = findViewById<TextView>(R.id.btnBack)

        cardRutin.setOnClickListener {
            selectedGoal = "rutin"
            btnLanjut.text = "Lanjut dengan Pelacakan Rutin →"
        }

        cardPromil.setOnClickListener {
            selectedGoal = "promil"
            btnLanjut.text = "Lanjut dengan Persiapan Kehamilan →"
        }

        btnLanjut.setOnClickListener {
            // Assuming LmpInputActivity exists or will be created
            val intent = Intent(this, LmpInputActivity::class.java)
            intent.putExtra("goal", selectedGoal)
            startActivity(intent)
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}
