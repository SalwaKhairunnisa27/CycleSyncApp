package com.example.cyclesyncapp.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cyclesyncapp.R
import com.example.cyclesyncapp.data.local.database.CycleDatabase
import com.example.cyclesyncapp.domain.usecase.PartnerSyncUseCase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PartnerSyncActivity : AppCompatActivity() {

    private val partnerSyncUseCase = PartnerSyncUseCase()
    private lateinit var database: CycleDatabase

    private var detectedPhaseName = "Fase Menstruasi"
    private var detectedDayNumber = 1
    private var detectedSymptoms: String? = null

    private lateinit var tvPhaseInfo: TextView
    private lateinit var tvMoodInfo: TextView
    private lateinit var etName: EditText
    private lateinit var etPhone: EditText
    private lateinit var tvPreview: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partner_sync)

        database = CycleDatabase.getDatabase(this)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        tvPhaseInfo = findViewById(R.id.tvPhaseInfo)
        tvMoodInfo = findViewById(R.id.tvMoodInfo)
        etName = findViewById(R.id.etPartnerName)
        etPhone = findViewById(R.id.etPartnerPhone)
        tvPreview = findViewById(R.id.tvWaPreview)

        // TextWatcher to update preview in real-time
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateMessagePreview()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        etName.addTextChangedListener(textWatcher)
        etPhone.addTextChangedListener(textWatcher)

        findViewById<Button>(R.id.btnSendWa).setOnClickListener {
            val message = tvPreview.text.toString()
            val phone = etPhone.text.toString()
            partnerSyncUseCase.shareStatusToWhatsApp(this, phone, message)
        }

        findViewById<Button>(R.id.btnCopyMsg).setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Partner Status", tvPreview.text.toString())
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Pesan disalin ke clipboard!", Toast.LENGTH_SHORT).show()
        }

        loadUserCycleAndSymptoms()
    }

    private fun loadUserCycleAndSymptoms() {
        lifecycleScope.launch {
            try {
                val prefs = getSharedPreferences("cycle_sync_prefs", MODE_PRIVATE)
                val activeEmail = prefs.getString("active_user_email", "aisyah@email.com") ?: "aisyah@email.com"
                val user = database.userDao().getUserByEmail(activeEmail) ?: database.userDao().getUser()

                val isPregnant = user?.isPregnant ?: false

                if (isPregnant) {
                    detectedPhaseName = "Fase Kehamilan 🤰"
                    detectedDayNumber = 0
                    tvPhaseInfo.text = detectedPhaseName
                    tvMoodInfo.text = "Mode Kehamilan: Menjaga kesehatan bersama pasangan"
                    detectedSymptoms = "Sedang menjaga kesehatan kehamilan"
                    updateMessagePreview()
                    return@launch
                }

                // Detect cycle phase
                val latestCycle = database.cycleDao().getLatestCycle()
                val periodLogs = database.dailyLogDao().getPeriodLogs()
                
                val periodStarts = mutableListOf<Long>()
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                
                if (latestCycle != null && latestCycle.notes != "UNKNOWN_LMP") {
                    try {
                        sdf.parse(latestCycle.startDate)?.time?.let { periodStarts.add(it) }
                    } catch (e: Exception) {}
                }
                
                if (periodLogs.isNotEmpty()) {
                    val sortedLogs = periodLogs.sortedBy { it.date }
                    val logTimes = sortedLogs.mapNotNull { log ->
                        try { sdf.parse(log.date)?.time } catch (e: Exception) { null }
                    }
                    for (i in logTimes.indices) {
                        if (i == 0 || logTimes[i] > logTimes[i - 1] + (1.5 * 24.0 * 60.0 * 60.0 * 1000.0).toLong()) {
                            periodStarts.add(logTimes[i])
                        }
                    }
                }

                val uniquePeriodStarts = periodStarts.distinct().sorted()

                if (uniquePeriodStarts.isEmpty()) {
                    // Fallback generic
                    detectedPhaseName = "Fase Hormonal"
                    detectedDayNumber = 1
                    tvPhaseInfo.text = "Menunggu data haid pertama..."
                    tvMoodInfo.text = "Gejala: Belum dicatat hari ini"
                    updateMessagePreview()
                    return@launch
                }

                val predictionResult = com.example.cyclesyncapp.domain.usecase.GetCyclePredictionUseCase().execute(uniquePeriodStarts, isPregnant)
                detectedDayNumber = predictionResult.dayOfCycle

                val phaseStr = when (predictionResult.currentPhase) {
                    com.example.cyclesyncapp.domain.model.HormonalPhase.MENSTRUATION -> "Menstruasi 🩸"
                    com.example.cyclesyncapp.domain.model.HormonalPhase.FOLLICULAR -> "Folikuler 🌱"
                    com.example.cyclesyncapp.domain.model.HormonalPhase.OVULATION -> "Ovulasi 🥚"
                    com.example.cyclesyncapp.domain.model.HormonalPhase.LUTEAL -> "Luteal 🌙"
                    else -> "Kehamilan 🤰"
                }

                detectedPhaseName = "Fase $phaseStr"
                tvPhaseInfo.text = "$detectedPhaseName · Hari ke-$detectedDayNumber"

                // Get today's symptoms from daily log if present
                val todayStr = sdf.format(Calendar.getInstance().time)
                database.dailyLogDao().getLogByDate(todayStr).collect { log ->
                    if (log != null) {
                        val symptomsText = log.symptoms ?: "Tidak ada keluhan fisik"
                        val flowText = if (log.flowLevel != null && log.flowLevel != "None") "Aliran: ${log.flowLevel}" else ""
                        val combined = listOf(flowText, symptomsText).filter { it.isNotEmpty() }.joinToString(", ")
                        
                        detectedSymptoms = combined
                        tvMoodInfo.text = "Gejala hari ini: $combined"
                    } else {
                        detectedSymptoms = null
                        tvMoodInfo.text = "Gejala hari ini: Belum dicatat"
                    }
                    updateMessagePreview()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateMessagePreview() {
        val partnerName = etName.text.toString()
        val message = partnerSyncUseCase.generateShareMessage(
            partnerName = partnerName,
            phase = detectedPhaseName,
            dayOfCycle = detectedDayNumber,
            symptoms = detectedSymptoms
        )
        tvPreview.text = message
    }
}