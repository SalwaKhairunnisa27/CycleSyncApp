package com.example.cyclesyncapp.ui.setup

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cyclesyncapp.data.local.database.CycleDatabase
import com.example.cyclesyncapp.data.local.entity.CycleEntity
import com.example.cyclesyncapp.data.local.entity.UserEntity
import com.example.cyclesyncapp.databinding.ActivitySetupBinding
import com.example.cyclesyncapp.ui.dashboard.DashboardActivity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private var nickname: String = "Aisyah"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Read existing nickname from SharedPreferences
        val mainPrefs = getSharedPreferences("cyclesync_prefs", MODE_PRIVATE)
        nickname = mainPrefs.getString("nickname", "Aisyah") ?: "Aisyah"
        if (nickname.isEmpty()) nickname = "Aisyah"

        // Initialize Partner Sync Form from SharedPreferences if available
        val partnerPrefs = getSharedPreferences("partner_prefs", MODE_PRIVATE)
        binding.etSetupPartnerName.setText(partnerPrefs.getString("name", ""))
        binding.etSetupPartnerPhone.setText(partnerPrefs.getString("phone", ""))

        updatePreview()

        // TextWatcher to update preview dynamically
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updatePreview()
            }
        }
        binding.etSetupPartnerName.addTextChangedListener(textWatcher)
        binding.etSetupPartnerPhone.addTextChangedListener(textWatcher)

        // Save Partner Contact Button
        binding.btnSetupSavePartner.setOnClickListener {
            val partnerName = binding.etSetupPartnerName.text.toString().trim()
            val partnerPhone = binding.etSetupPartnerPhone.text.toString().trim()

            partnerPrefs.edit().apply {
                putString("name", partnerName)
                putString("phone", partnerPhone)
                apply()
            }
            Toast.makeText(this, "Kontak Pasangan Tersimpan!", Toast.LENGTH_SHORT).show()
        }

        // Start Journey / Complete Button
        binding.btnStartJourney.setOnClickListener {
            // Save Notification Preferences
            val notifDailyLog = binding.switchDailyLog.isChecked
            val notifPeriod = binding.switchPeriod.isChecked
            val notifFertile = binding.switchFertile.isChecked
            val notifSupplement = binding.switchSupplement.isChecked

            mainPrefs.edit().apply {
                putBoolean("setup_done", true)
                putBoolean("notif_daily_log", notifDailyLog)
                putBoolean("notif_period", notifPeriod)
                putBoolean("notif_fertility", notifFertile)
                putBoolean("notif_supplement", notifSupplement)
                apply()
            }

            // Async database insert
            lifecycleScope.launch {
                val db = CycleDatabase.getDatabase(this@SetupActivity)

                // 1. Insert User Profile details
                val age = mainPrefs.getInt("age", 25)
                val height = mainPrefs.getFloat("height", 160.0f).toDouble()
                val weight = mainPrefs.getFloat("weight", 55.0f).toDouble()
                val cycleLength = mainPrefs.getInt("cycle_length", 28)

                val user = UserEntity(
                    name = nickname,
                    age = age,
                    height = height,
                    weight = weight,
                    cycleLength = cycleLength
                )
                db.userDao().insertUser(user)

                // 2. Insert Initial Cycle Entry
                val lastPeriodStr = mainPrefs.getString("last_period", "") ?: ""
                val periodDuration = mainPrefs.getInt("period_duration", 5)

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val cal = Calendar.getInstance()
                if (lastPeriodStr.isNotEmpty()) {
                    try {
                        val parsedDate = sdf.parse(lastPeriodStr)
                        if (parsedDate != null) {
                            cal.time = parsedDate
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                val startDateStr = sdf.format(cal.time)
                cal.add(Calendar.DAY_OF_YEAR, periodDuration)
                val endDateStr = sdf.format(cal.time)

                val cycle = CycleEntity(
                    startDate = startDateStr,
                    endDate = endDateStr,
                    cycleLength = cycleLength,
                    periodLength = periodDuration,
                    notes = "Initial onboarding cycle"
                )
                db.cycleDao().insertCycle(cycle)

                // 3. Complete and Open Dashboard
                val intent = Intent(this@SetupActivity, DashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    private fun updatePreview() {
        val partnerName = binding.etSetupPartnerName.text.toString().trim()
        val displayName = if (partnerName.isNotEmpty()) partnerName else "Pasangan"
        
        val previewText = "CycleSync Update dari $nickname 🌸\n\nHai sayang, FYI aku lagi di fase Luteal nih.\nMoodku lagi Tenang 😌, energi Sedang.\n\n_Pesan otomatis dari CycleSync_"
        binding.tvSetupPreviewMessage.text = previewText
    }
}