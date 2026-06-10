package com.example.cyclesyncapp.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cyclesyncapp.R
import com.example.cyclesyncapp.data.local.database.CycleDatabase
import com.example.cyclesyncapp.data.repository.UserRepositoryImpl
import com.example.cyclesyncapp.domain.usecase.PregnancyTransitionUseCase
import kotlinx.coroutines.launch

class PostpartumActivity : AppCompatActivity() {

    private lateinit var pregnancyTransitionUseCase: PregnancyTransitionUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_postpartum)

        val database = CycleDatabase.getDatabase(this)
        val userRepository = UserRepositoryImpl(database.userDao())
        pregnancyTransitionUseCase = PregnancyTransitionUseCase(userRepository)

        findViewById<Button>(R.id.btnEndPregnancy).setOnClickListener {
            endPregnancy()
        }

        findViewById<Button>(R.id.btnCancel).setOnClickListener {
            finish()
        }
    }

    private fun endPregnancy() {
        lifecycleScope.launch {
            pregnancyTransitionUseCase.switchToNormalCycle()
            Toast.makeText(this@PostpartumActivity, "Mode Siklus Normal Aktif!", Toast.LENGTH_SHORT).show()
            
            val intent = Intent(this@PostpartumActivity, PostpartumDashboardActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
    }
}