package com.example.cyclesyncapp.ui.dashboard

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.lifecycleScope
import com.example.cyclesyncapp.R
import com.example.cyclesyncapp.data.local.database.CycleDatabase
import com.example.cyclesyncapp.data.local.entity.DailyLogEntity
import com.example.cyclesyncapp.data.security.EncryptionManager
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DailyLogActivity : AppCompatActivity() {

    private lateinit var database: CycleDatabase
    private lateinit var currentSelectedCalendar: Calendar
    private var logCollectJob: Job? = null
    
    private lateinit var tvLogDate: TextView
    private lateinit var tvDailyLogTitle: TextView
    private lateinit var moodSlider: SeekBar
    private lateinit var cardMoodColor: androidx.cardview.widget.CardView
    private lateinit var layoutMoodColor: LinearLayout
    private lateinit var tvMoodFeeling: TextView
    
    private lateinit var btnMoodOverall: TextView
    private lateinit var btnMoodCurrent: TextView
    private lateinit var tvMoodTime: TextView
    
    private lateinit var energySlider: SeekBar
    private lateinit var tvEnergyLabel: TextView
    private lateinit var etNotes: EditText
    
    private lateinit var cardPregnancyTips: androidx.cardview.widget.CardView
    private lateinit var tvPregnancyTipContent: TextView
    
    private lateinit var layoutSavedOverlay: View
    private lateinit var btnSave: Button

    private var feelingChips = mapOf<String, Chip>()

    private var existingFlowLevel: String? = null
    private var existingSymptoms: String? = null
    private var existingPhase: String = "UNKNOWN"
    private var existingLogId: Int = 0
    
    private var activeEmail: String = "aisyah@email.com"
    private var isPregnant: Boolean = false
    private var selectedMoodType: String = "OVERALL" // "OVERALL" or "CURRENT"
    private var selectedMoodTime: String = "--:--"
    private lateinit var cardDailyLogTutorialGuide: androidx.cardview.widget.CardView
    private var isTutorialMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_log)

        database = CycleDatabase.getDatabase(this)
        currentSelectedCalendar = Calendar.getInstance()

        // Bind Views
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        tvLogDate = findViewById(R.id.tvLogDate)
        tvDailyLogTitle = findViewById(R.id.tvDailyLogTitle)
        moodSlider = findViewById(R.id.moodSlider)
        cardMoodColor = findViewById(R.id.cardMoodColor)
        layoutMoodColor = findViewById(R.id.layoutMoodColor)
        tvMoodFeeling = findViewById(R.id.tvMoodFeeling)
        
        btnMoodOverall = findViewById(R.id.btnMoodOverall)
        btnMoodCurrent = findViewById(R.id.btnMoodCurrent)
        tvMoodTime = findViewById(R.id.tvMoodTime)
        
        energySlider = findViewById(R.id.energySlider)
        tvEnergyLabel = findViewById(R.id.tvEnergyLabel)
        etNotes = findViewById(R.id.etNotes)
        
        cardPregnancyTips = findViewById(R.id.cardPregnancyTips)
        tvPregnancyTipContent = findViewById(R.id.tvPregnancyTipContent)
        cardDailyLogTutorialGuide = findViewById(R.id.cardDailyLogTutorialGuide)

        val prefs = getSharedPreferences("cycle_sync_prefs", MODE_PRIVATE)
        val onboardingStep = prefs.getInt("onboarding_step", 0)
        isTutorialMode = onboardingStep == 2
        if (isTutorialMode) {
            cardDailyLogTutorialGuide.visibility = View.VISIBLE
        } else {
            cardDailyLogTutorialGuide.visibility = View.GONE
        }

        layoutSavedOverlay = findViewById(R.id.layoutSavedOverlay)
        btnSave = findViewById(R.id.btnSave)

        // Bind Feeling Chips
        feelingChips = mapOf(
            "Senang" to findViewById(R.id.chipHappy),
            "Tenang" to findViewById(R.id.chipCalm),
            "Cemas" to findViewById(R.id.chipAnxious),
            "Sedih" to findViewById(R.id.chipSad),
            "Marah" to findViewById(R.id.chipAngry),
            "Terkejut" to findViewById(R.id.chipSurprised),
            "Lelah" to findViewById(R.id.chipTired),
            "Bersemangat" to findViewById(R.id.chipExcited),
            "Stres" to findViewById(R.id.chipStressed)
        )

        // Setup SeekBars
        setupMoodSlider()
        setupEnergySlider()
        setupMoodTypeToggles()

        // Setup Date Navigation
        findViewById<TextView>(R.id.btnPrevDay).setOnClickListener {
            navigateDay(-1)
        }
        findViewById<TextView>(R.id.btnNextDay).setOnClickListener {
            navigateDay(1)
        }

        // Setup Save Button
        btnSave.setOnClickListener {
            saveDailyLog()
        }

        // Load active user session & initialize date UI
        checkUserSession()
    }

    private fun checkUserSession() {
        lifecycleScope.launch {
            val prefs = getSharedPreferences("cycle_sync_prefs", MODE_PRIVATE)
            activeEmail = prefs.getString("active_user_email", "aisyah@email.com") ?: "aisyah@email.com"
            val user = database.userDao().getUserByEmail(activeEmail) ?: database.userDao().getUser()
            
            isPregnant = user?.isPregnant ?: false
            
            if (isPregnant) {
                tvDailyLogTitle.text = "Log Harian Kehamilan"
                cardPregnancyTips.visibility = View.VISIBLE
                
                // Customize tip based on gestational age
                val latestCycle = database.cycleDao().getLatestCycle()
                if (latestCycle != null) {
                    val startParts = latestCycle.startDate.split("-")
                    if (startParts.size == 3) {
                        val today = Calendar.getInstance()
                        val lmp = Calendar.getInstance().apply {
                            set(startParts[0].toInt(), startParts[1].toInt() - 1, startParts[2].toInt())
                        }
                        val diffInMillis = today.timeInMillis - lmp.timeInMillis
                        val totalDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
                        val weeks = totalDays / 7
                        
                        when {
                            weeks <= 12 -> tvPregnancyTipContent.text = "Trimester 1: Pembentukan organ vital bayi sedang berlangsung. Konsumsi asam folat, hindari kelelahan berlebih, dan tidur yang cukup."
                            weeks <= 27 -> tvPregnancyTipContent.text = "Trimester 2: Energi biasanya mulai kembali normal. Lakukan olahraga ringan seperti prenatal yoga untuk melatih otot panggul."
                            else -> tvPregnancyTipContent.text = "Trimester 3: Persiapan menjelang kelahiran. Fokus pada latihan pernapasan, relaksasi otot, dan jaga kecukupan cairan tubuh Anda."
                        }
                    }
                }
            } else {
                tvDailyLogTitle.text = "Log Harian"
                cardPregnancyTips.visibility = View.GONE
            }

            updateDateUI()
        }
    }

    private fun navigateDay(offset: Int) {
        currentSelectedCalendar.add(Calendar.DAY_OF_YEAR, offset)
        updateDateUI()
    }

    private fun updateDateUI() {
        val displayFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
        tvLogDate.text = displayFormat.format(currentSelectedCalendar.time)
        loadLogForDate()
    }

    private fun loadLogForDate() {
        logCollectJob?.cancel()

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(currentSelectedCalendar.time)

        logCollectJob = lifecycleScope.launch {
            database.dailyLogDao().getLogByDate(dateStr).collect { log ->
                if (log != null) {
                    existingLogId = log.id
                    existingFlowLevel = log.flowLevel
                    existingSymptoms = log.symptoms
                    existingPhase = log.phase

                    // Set feeling chips from symptoms column
                    val symptomsList = log.symptoms?.split(", ") ?: emptyList()
                    feelingChips.forEach { (name, chip) ->
                        chip.isChecked = symptomsList.contains(name)
                    }

                    // Decrypt and parse note
                    try {
                        val decrypted = EncryptionManager.decrypt(log.encryptedNote)
                        parseDecryptedNote(decrypted)
                    } catch (e: Exception) {
                        resetToDefaultInputs()
                    }
                } else {
                    existingLogId = 0
                    existingFlowLevel = null
                    existingSymptoms = null
                    existingPhase = "UNKNOWN"
                    resetToDefaultInputs()
                }
            }
        }
    }

    private fun parseDecryptedNote(decryptedText: String) {
        var moodVal = 50
        var energyVal = 2
        var noteText = ""
        var type = "OVERALL"
        var time = "--:--"

        if (decryptedText.contains("MoodSeekbar:")) {
            val moodRegex = "MoodSeekbar: (\\d+)".toRegex()
            val energyRegex = "EnergySeekbar: (\\d+)".toRegex()
            val typeRegex = "MoodType: (\\w+)".toRegex()
            val timeRegex = "MoodTime: ([\\d:]+)".toRegex()
            val notesRegex = "Notes: (.*)".toRegex()

            moodVal = moodRegex.find(decryptedText)?.groupValues?.get(1)?.toIntOrNull() ?: 50
            energyVal = energyRegex.find(decryptedText)?.groupValues?.get(1)?.toIntOrNull() ?: 2
            type = typeRegex.find(decryptedText)?.groupValues?.get(1) ?: "OVERALL"
            time = timeRegex.find(decryptedText)?.groupValues?.get(1) ?: "--:--"
            noteText = notesRegex.find(decryptedText)?.groupValues?.get(1) ?: ""
        } else {
            // Old format backward compatibility parser
            val oldEnergyRegex = "Energi: (\\d)/5".toRegex()
            val oldNotesRegex = "Cerita: (.*)".toRegex()
            val oldMoodRegex = "Mood: (\\w+)".toRegex()

            val oldMood = oldMoodRegex.find(decryptedText)?.groupValues?.get(1) ?: ""
            moodVal = when (oldMood) {
                "Bahagia", "Happy" -> 85
                "Tenang", "Calm" -> 60
                "Sedih", "Sad" -> 30
                "Emosional", "Angry" -> 15
                "Lelah", "Tired" -> 25
                else -> 50
            }

            val rawEnergy = oldEnergyRegex.find(decryptedText)?.groupValues?.get(1)?.toIntOrNull() ?: 3
            energyVal = (rawEnergy - 1).coerceIn(0, 4)
            noteText = oldNotesRegex.find(decryptedText)?.groupValues?.get(1) ?: ""
        }

        moodSlider.progress = moodVal
        updateMoodColor(moodVal)

        energySlider.progress = energyVal
        updateEnergyLabel(energyVal)

        setMoodType(type, time)

        etNotes.setText(noteText)
    }

    private fun resetToDefaultInputs() {
        moodSlider.progress = 50
        updateMoodColor(50)

        energySlider.progress = 2
        updateEnergyLabel(2)

        setMoodType("OVERALL", "--:--")
        
        feelingChips.values.forEach { it.isChecked = false }

        etNotes.setText("")
    }

    private fun setupMoodSlider() {
        moodSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateMoodColor(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        updateMoodColor(50) // Init
    }

    private fun updateMoodColor(progress: Int) {
        // Interpolate colors: Unpleasant (Purple #7B5EA7) -> Neutral (White #FFFFFF) -> Pleasant (Pink #D4607A)
        val color = if (progress < 50) {
            val fraction = progress / 50f
            ColorUtils.blendARGB(Color.parseColor("#7B5EA7"), Color.WHITE, fraction)
        } else {
            val fraction = (progress - 50f) / 50f
            ColorUtils.blendARGB(Color.WHITE, Color.parseColor("#D4607A"), fraction)
        }

        // Apply background color to the card to preserve rounded corners
        cardMoodColor.setCardBackgroundColor(color)

        // Describe mood text based on slider
        val desc = when {
            progress <= 20 -> "Sangat Tidak Nyaman (Marah / Frustrasi)"
            progress <= 40 -> "Kurang Nyaman (Cemas / Lelah)"
            progress <= 60 -> "Netral (Tenang / Biasa Saja)"
            progress <= 80 -> "Nyaman (Senang / Aktif)"
            else -> "Sangat Nyaman (Gembira / Penuh Energi)"
        }
        tvMoodFeeling.text = desc

        // Adapt text color to dark/light backgrounds for high contrast readability
        if (progress <= 25 || progress >= 75) {
            tvMoodFeeling.setTextColor(Color.WHITE)
        } else {
            tvMoodFeeling.setTextColor(Color.parseColor("#2C1A20")) // pt color
        }
    }

    private fun setupEnergySlider() {
        energySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateEnergyLabel(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        updateEnergyLabel(2) // Init
    }

    private fun updateEnergyLabel(progress: Int) {
        val label = when (progress) {
            0 -> "Sangat Lemah (1/5)"
            1 -> "Lemah (2/5)"
            2 -> "Sedang (3/5)"
            3 -> "Kuat (4/5)"
            else -> "Sangat Kuat (5/5)"
        }
        tvEnergyLabel.text = label
    }

    private fun setupMoodTypeToggles() {
        btnMoodOverall.setOnClickListener {
            setMoodType("OVERALL")
        }
        btnMoodCurrent.setOnClickListener {
            // Get current time
            val cal = Calendar.getInstance()
            val timeStr = String.format(Locale.US, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
            setMoodType("CURRENT", timeStr)
        }
    }

    private fun setMoodType(type: String, time: String = "--:--") {
        selectedMoodType = type
        if (type == "OVERALL") {
            btnMoodOverall.setBackgroundResource(R.drawable.bg_pill_pink)
            btnMoodOverall.setTextColor(ContextCompat.getColor(this, R.color.white))
            
            btnMoodCurrent.setBackgroundResource(R.drawable.bg_quick_card)
            btnMoodCurrent.setTextColor(ContextCompat.getColor(this, R.color.pm))
            
            tvMoodTime.visibility = View.GONE
        } else {
            btnMoodOverall.setBackgroundResource(R.drawable.bg_quick_card)
            btnMoodOverall.setTextColor(ContextCompat.getColor(this, R.color.pm))
            
            btnMoodCurrent.setBackgroundResource(R.drawable.bg_pill_pink)
            btnMoodCurrent.setTextColor(ContextCompat.getColor(this, R.color.white))
            
            selectedMoodTime = time
            tvMoodTime.text = "Pencatatan Pukul $time"
            tvMoodTime.visibility = View.VISIBLE
        }
    }

    private fun saveDailyLog() {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(currentSelectedCalendar.time)
        val notesText = etNotes.text.toString()
        val moodProgress = moodSlider.progress
        val energyLevel = energySlider.progress

        // Gather feeling chips
        val selectedFeelings = mutableListOf<String>()
        feelingChips.forEach { (name, chip) ->
            if (chip.isChecked) {
                selectedFeelings.add(name)
            }
        }
        val feelingsString = selectedFeelings.joinToString(", ")

        val fullNoteText = "MoodSeekbar: $moodProgress, EnergySeekbar: $energyLevel, MoodType: $selectedMoodType, MoodTime: $selectedMoodTime, Notes: $notesText"
        val encryptedData = EncryptionManager.encrypt(fullNoteText)

        val newLog = DailyLogEntity(
            id = existingLogId,
            date = dateStr,
            flowLevel = existingFlowLevel,
            symptoms = if (feelingsString.isNotEmpty()) feelingsString else null,
            encryptedNote = encryptedData,
            phase = existingPhase
        )

        lifecycleScope.launch {
            try {
                database.dailyLogDao().insertLog(newLog)
                
                // Show cute success overlay animation
                showSuccessOverlay()
            } catch (e: Exception) {
                Toast.makeText(this@DailyLogActivity, "Gagal menyimpan catatan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSuccessOverlay() {
        if (isTutorialMode) {
            val prefs = getSharedPreferences("cycle_sync_prefs", MODE_PRIVATE)
            prefs.edit().putInt("onboarding_step", 3).apply()
        }

        layoutSavedOverlay.visibility = View.VISIBLE
        val anim = AlphaAnimation(0.0f, 1.0f).apply {
            duration = 300
        }
        layoutSavedOverlay.startAnimation(anim)

        // After 1.5 seconds, fade out and close
        layoutSavedOverlay.postDelayed({
            val fadeOut = AlphaAnimation(1.0f, 0.0f).apply {
                duration = 300
            }
            layoutSavedOverlay.startAnimation(fadeOut)
            layoutSavedOverlay.visibility = View.GONE
            finish()
        }, 1500)
    }
}