package com.example.cyclesyncapp.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
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

        // Find inner layouts to change background
        val layoutRutin = cardRutin.getChildAt(0) as LinearLayout
        val layoutPromil = cardPromil.getChildAt(0) as LinearLayout

        fun updateSelection() {
            if (selectedGoal == "rutin") {
                layoutRutin.setBackgroundResource(R.drawable.bg_card_selected)
                layoutPromil.setBackgroundResource(R.drawable.bg_card)
                btnLanjut.text = "Lanjut dengan Pelacakan Rutin →"
            } else {
                layoutRutin.setBackgroundResource(R.drawable.bg_card)
                layoutPromil.setBackgroundResource(R.drawable.bg_card_selected)
                btnLanjut.text = "Lanjut dengan Persiapan Kehamilan →"
            }
        }

        cardRutin.setOnClickListener {
            selectedGoal = "rutin"
            updateSelection()
        }

        cardPromil.setOnClickListener {
            selectedGoal = "promil"
            updateSelection()
        }

        btnLanjut.setOnClickListener {
            val intent = Intent(this, LmpInputActivity::class.java).apply {
                putExtra("goal", selectedGoal)
                putExtra("nickname", getIntent().getStringExtra("nickname"))
            }
            startActivity(intent)
        }

        btnBack.setOnClickListener {
            finish()
        }
        
        updateSelection()
    }
}

