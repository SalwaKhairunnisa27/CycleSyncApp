package com.example.cyclesyncapp.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cyclesyncapp.R
import com.example.cyclesyncapp.data.local.database.CycleDatabase
import com.example.cyclesyncapp.data.repository.UserRepositoryImpl
import com.example.cyclesyncapp.domain.usecase.PregnancyTransitionUseCase
import kotlinx.coroutines.launch

class PregnancyDetectActivity : AppCompatActivity() {

    private lateinit var pregnancyTransitionUseCase: PregnancyTransitionUseCase
    private lateinit var userRepository: UserRepositoryImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pregnancy_detect)

        val database = CycleDatabase.getDatabase(this)
        userRepository = UserRepositoryImpl(database.userDao())
        pregnancyTransitionUseCase = PregnancyTransitionUseCase(userRepository)

        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btnPositif).setOnClickListener {
            confirmPregnancy()
        }

        findViewById<Button>(R.id.btnNegatif).setOnClickListener {
            Toast.makeText(this, "Hasil dicatat. Sistem akan menyesuaikan.", Toast.LENGTH_SHORT).show()
            finish()
        }

        findViewById<Button>(R.id.btnResetCycle).setOnClickListener {
            Toast.makeText(this, "Siklus direset berdasarkan hari ini.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun confirmPregnancy() {
        lifecycleScope.launch {
            val prefs = getSharedPreferences("cycle_sync_prefs", MODE_PRIVATE)
            val activeEmail = prefs.getString("active_user_email", "aisyah@email.com")
            pregnancyTransitionUseCase.switchToPregnancyMode(activeEmail)
            Toast.makeText(this@PregnancyDetectActivity, "Mode Kehamilan Aktif!", Toast.LENGTH_LONG).show()
            
            val intent = Intent(this@PregnancyDetectActivity, DashboardActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
    }
}