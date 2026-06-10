package com.example.cyclesyncapp.ui.onboarding

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.cyclesyncapp.R
import com.example.cyclesyncapp.data.local.database.CycleDatabase
import com.example.cyclesyncapp.data.local.entity.CycleEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class LmpInputActivity : AppCompatActivity() {
    private var selectedLmp: Calendar? = null
    private var selectedDuration: Int = 5
    private var isEstimated: Boolean = false
    private var isUnknownLmp: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lmp_input)

        val database = CycleDatabase.getDatabase(this)
        val btnHitung = findViewById<Button>(R.id.btnHitung)
        val tvLmpDate = findViewById<TextView>(R.id.tvLmpDate)
        val tvForgotLmp = findViewById<TextView>(R.id.tvForgotLmp)

        tvLmpDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(this, { _, year, month, day ->
                val selected = Calendar.getInstance()
                selected.set(year, month, day)
                selectedLmp = selected
                isEstimated = false
                isUnknownLmp = false
                tvLmpDate.text = "$day/${month + 1}/$year"
                tvLmpDate.setTextColor(ContextCompat.getColor(this, R.color.primary_text))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            datePickerDialog.show()
        }

        tvForgotLmp.setOnClickListener {
            isUnknownLmp = true
            selectedLmp = null
            isEstimated = false
            tvLmpDate.text = "Tidak Diketahui"
            tvLmpDate.setTextColor(ContextCompat.getColor(this, R.color.primary))
        }

        btnHitung.setOnClickListener {
            if (selectedLmp == null && !isUnknownLmp) {
                Toast.makeText(this, "Silakan pilih tanggal atau klik 'Saya lupa tanggalnya'", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val goal = intent.getStringExtra("goal") ?: "rutin"
            val nickname = intent.getStringExtra("nickname") ?: "Aisyah"

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val lmpString = if (isUnknownLmp) "UNKNOWN_LMP" else dateFormat.format(selectedLmp!!.time)
            val endString = if (isUnknownLmp) "UNKNOWN_LMP" else {
                val endCal = selectedLmp!!.clone() as Calendar
                endCal.add(Calendar.DAY_OF_YEAR, selectedDuration)
                dateFormat.format(endCal.time)
            }

            lifecycleScope.launch {
                try {
                    // Update profile for the currently active user session
                    val prefs = getSharedPreferences("cycle_sync_prefs", MODE_PRIVATE)
                    val activeEmail = prefs.getString("active_user_email", "aisyah@email.com") ?: "aisyah@email.com"
                    val user = database.userDao().getUserByEmail(activeEmail) ?: database.userDao().getUser()
                    
                    if (user != null) {
                        val updatedUser = user.copy(
                            name = nickname,
                            cycleLength = 28, // Default 28 days
                            isPregnant = false
                        )
                        database.userDao().updateUser(updatedUser)
                    }

                    // Save onboarding_lmp to SharedPreferences
                    prefs.edit().putString("onboarding_lmp", lmpString).apply()

                    // Save cycle data to database
                    database.cycleDao().insertCycle(
                        CycleEntity(
                            startDate = lmpString,
                            endDate = endString,
                            cycleLength = 28,
                            periodLength = selectedDuration,
                            notes = if (isUnknownLmp) "UNKNOWN_LMP" else "USER_INPUT"
                        )
                    )

                    val intent = Intent(this@LmpInputActivity, CalculatingActivity::class.java)
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@LmpInputActivity, "Gagal menyimpan data", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val durationButtons = mapOf(
            3 to findViewById<TextView>(R.id.btn3hr),
            4 to findViewById<TextView>(R.id.btn4hr),
            5 to findViewById<TextView>(R.id.btn5hr),
            6 to findViewById<TextView>(R.id.btn6hr),
            7 to findViewById<TextView>(R.id.btn7hr)
        )

        durationButtons.forEach { (days, btn) ->
            btn?.setOnClickListener {
                selectedDuration = days
                durationButtons.values.forEach { 
                    it?.setTextColor(ContextCompat.getColor(this, R.color.primary_text))
                    it?.setBackgroundResource(R.drawable.bg_card) 
                }
                btn.setTextColor(ContextCompat.getColor(this, R.color.primary))
                btn.setBackgroundResource(R.drawable.bg_pill_pink)
            }
        }
    }
}
