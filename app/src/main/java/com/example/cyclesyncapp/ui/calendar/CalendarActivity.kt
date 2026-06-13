package com.example.cyclesyncapp.ui.calendar

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.example.cyclesyncapp.R
import com.example.cyclesyncapp.data.local.database.CycleDatabase
import com.example.cyclesyncapp.data.local.entity.CycleEntity
import com.example.cyclesyncapp.data.local.entity.DailyLogEntity
import com.example.cyclesyncapp.data.security.EncryptionManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.example.cyclesyncapp.databinding.ActivityCalendarBinding
import com.example.cyclesyncapp.ui.dashboard.PregnancyDetectActivity
import com.example.cyclesyncapp.ui.viewmodel.CycleViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class CalendarDay(
    val date: Date?,
    val dayNumber: String,
    val isSelected: Boolean = false,
    val isToday: Boolean = false,
    val hasLog: Boolean = false,
    val flowLevel: String? = null,
    val isPredictedPeriod: Boolean = false,
    val isPredictedFertile: Boolean = false,
    val isPredictedValuesLoaded: Boolean = false,
    val isPredictedOvulation: Boolean = false,
    val pregnancyWeek: Int? = null,
    val trimester: Int? = null
)

class CalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarBinding
    private lateinit var database: CycleDatabase
    private lateinit var behavior: BottomSheetBehavior<NestedScrollView>
    private lateinit var currentSelectedCalendar: Calendar
    private lateinit var currentMonthCalendar: Calendar

    private val cycleViewModel: CycleViewModel by viewModels()

    private var currentFlowLevel: String = "None"
    private var currentLogId: Int = 0
    private var logCollectJob: Job? = null
    private var calendarGenerateJob: Job? = null

    private var isPregnant: Boolean = false
    private var activeEmail: String = "aisyah@email.com"
    private var isTutorialMode: Boolean = false
    private var isPregTutorialMode: Boolean = false
    private var isBinding: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = CycleDatabase.getDatabase(this)
        
        // Setup Bottom Sheet
        behavior = BottomSheetBehavior.from(binding.trackingSheet)
        behavior.state = BottomSheetBehavior.STATE_HIDDEN

        currentSelectedCalendar = Calendar.getInstance()
        currentMonthCalendar = Calendar.getInstance()
        currentMonthCalendar.set(Calendar.DAY_OF_MONTH, 1)

        val prefs = getSharedPreferences("cycle_sync_prefs", MODE_PRIVATE)
        val onboardingStep = prefs.getInt("onboarding_step", 0)
        val pregnancyStep = prefs.getInt("pregnancy_onboarding_step", 0)
        isTutorialMode = onboardingStep == 1 || pregnancyStep == 1
        isPregTutorialMode = pregnancyStep == 1
        if (isTutorialMode) {
            binding.cardCalendarTutorialGuide.visibility = View.VISIBLE
            if (isPregTutorialMode) {
                binding.tvCalendarTutorialTitle.text = "Langkah 2 dari 4: Catat Gejala Kehamilan"
                binding.tvCalendarTutorialDesc.text = "💡 Ketuk tombol 'Log Fase & Gejala' di bawah, lalu pilih minimal 1 gejala kehamilan (misalnya 'Mual') dan klik 'Selesai & Simpan' untuk merekam kondisi hari ini."
            }
            updateTutorialGuideText()
            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    updateTutorialGuideText()
                }
                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
            })
        } else {
            binding.cardCalendarTutorialGuide.visibility = View.GONE
        }

        // Set awal UI tanggal
        setDate(currentSelectedCalendar)

        // Setup Cycle History RecyclerView & Adapter
        val historyAdapter = CycleHistoryAdapter()
        binding.rvCycleHistory.adapter = historyAdapter
        binding.rvCycleHistory.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        // Fetch active user session and determine if pregnant
        lifecycleScope.launch {
            val prefs = getSharedPreferences("cycle_sync_prefs", MODE_PRIVATE)
            activeEmail = prefs.getString("active_user_email", "aisyah@email.com") ?: "aisyah@email.com"
            val user = database.userDao().getUserByEmail(activeEmail) ?: database.userDao().getUser()
            isPregnant = user?.isPregnant ?: false

            if (isPregnant) {
                setupPregnancyUI()
            }

            // Load predictions and history
            cycleViewModel.loadPredictionData(isPregnant)
        }

        // Observe Cycle Predictions and update calendar views
        cycleViewModel.predictionResult.observe(this) { result ->
            if (result == null) return@observe

            if (isPregnant) {
                setupPregnancyUI()
                binding.cardLatePeriodAlert.visibility = View.GONE
                // Fetch pregnancy LMP and calculate weeks/days + HPL
                lifecycleScope.launch {
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
                            val days = totalDays % 7
                            
                            binding.tvAvgCycle.text = "${weeks}w ${days}d"
                            
                            val hplCal = lmp.clone() as Calendar
                            hplCal.add(Calendar.DAY_OF_YEAR, 280)
                            val dayOfMonth = hplCal.get(Calendar.DAY_OF_MONTH)
                            val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
                            val hplStr = "$dayOfMonth ${monthNames[hplCal.get(Calendar.MONTH)]}"
                            binding.tvNextPeriod.text = hplStr

                            binding.tvAvgCycleLabel.text = "Fase Kehamilan"
                            binding.tvNextPeriodLabel.text = "Perkiraan Lahir (HPL)"

                            val data = fetalDevelopmentData[weeks.coerceIn(0, 40)] ?: Pair("Janin berkembang", "Tetap jaga kesehatan Anda.")
                            binding.tvFetalWeekSubtitle.text = "Minggu Ke-$weeks Kehamilan"
                            binding.tvFetalSize.text = "Perbandingan Ukuran: ${data.first}"
                            binding.tvFetalMilestone.text = "Milestone: ${data.second}"
                            binding.cardFetalDevelopment.visibility = View.VISIBLE
                        }
                    } else {
                        binding.tvAvgCycle.text = "--"
                        binding.tvNextPeriod.text = "--"
                        binding.tvAvgCycleLabel.text = "Fase Kehamilan"
                        binding.tvNextPeriodLabel.text = "Perkiraan Lahir (HPL)"
                        binding.cardFetalDevelopment.visibility = View.GONE
                    }
                }
                binding.cardCalendarAccuracy.visibility = View.GONE
                generateCalendarDays(currentMonthCalendar)
                return@observe
            }

            // Normal cycle mode UI updates
            binding.tvAvgCycleLabel.text = "Rata-rata siklus"
            binding.tvNextPeriodLabel.text = "Haid berikutnya"
            binding.cardCalendarAccuracy.visibility = View.VISIBLE
            binding.layoutTrimesterLegend.visibility = View.GONE
            binding.cardFetalDevelopment.visibility = View.GONE
            binding.cardColorLegend.visibility = View.VISIBLE
            binding.cardCycleHistory.visibility = View.VISIBLE
            
            binding.tvAvgCycle.text = "${result.averageCycle} hr"
            
            // Restore pink theme
            binding.header.setBackgroundColor(ContextCompat.getColor(this, R.color.pb2))
            binding.btnCycleTracking.backgroundTintList = ContextCompat.getColorStateList(this, R.color.p)
            binding.tvCycleDataCount.setTextColor(ContextCompat.getColor(this, R.color.p))
            binding.tvCycleDataCount.visibility = View.VISIBLE
            
            if (result.hasPrediction && result.nextPeriodDate > 0L) {
                val outputFormat = SimpleDateFormat("d MMM", Locale("id", "ID"))
                
                // Show prediction range e.g. "12 Feb - 16 Feb"
                val rangeStartCal = Calendar.getInstance().apply { timeInMillis = result.predictionRangeStart }
                val rangeEndCal = Calendar.getInstance().apply { timeInMillis = result.predictionRangeEnd }
                val rangeStr = "${outputFormat.format(rangeStartCal.time)} - ${outputFormat.format(rangeEndCal.time)}"
                binding.tvNextPeriod.text = rangeStr
                
                binding.tvHeaderSub.text = "Prediksi Siklus Pintar · ${SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(currentMonthCalendar.time)}"
                
                // Check if Period is Late > 14 days
                val today = Calendar.getInstance()
                val diff = result.nextPeriodDate - today.timeInMillis
                val daysRemaining = (diff / (24 * 60 * 60 * 1000)).toInt()
                val daysLate = -daysRemaining
                
                if (daysLate > 14) {
                    binding.cardLatePeriodAlert.visibility = View.VISIBLE
                    binding.tvAlertSubtitle.text = "Siklus haid Anda terlambat $daysLate hari. Sangat disarankan untuk melakukan tes kehamilan."
                    binding.cardLatePeriodAlert.setOnClickListener {
                        startActivity(Intent(this, PregnancyDetectActivity::class.java))
                    }
                } else {
                    binding.cardLatePeriodAlert.visibility = View.GONE
                    binding.cardLatePeriodAlert.setOnClickListener(null)
                }
            } else {
                binding.tvNextPeriod.text = "--"
                binding.tvHeaderSub.text = "Prediksi Siklus Pintar · ${SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(currentMonthCalendar.time)}"
                binding.cardLatePeriodAlert.visibility = View.GONE
                binding.cardLatePeriodAlert.setOnClickListener(null)
            }

            // Update Prediction Accuracy views
            binding.tvCalendarConfidenceBadge.text = result.confidenceLevel
            binding.tvCalendarConfidenceBadge.setBackgroundResource(
                when (result.confidenceLevel) {
                    "Sangat Akurat" -> R.drawable.bg_pill_pink
                    "Cukup Akurat" -> R.drawable.bg_pill_soft
                    else -> R.drawable.badge_white_trans
                }
            )
            binding.tvCalendarConfidenceBadge.setTextColor(
                if (result.confidenceLevel == "Data Terbatas") ContextCompat.getColor(this, R.color.p)
                else ContextCompat.getColor(this, if (result.confidenceLevel == "Cukup Akurat") R.color.p else R.color.white)
            )
            
            binding.tvCalendarRegularity.text = "Keteraturan: ${result.regularityStatus}"
            binding.tvCalendarProgressText.text = result.progressText
            binding.pbCalendarAccuracy.progress = result.progressPercent
            binding.tvCalendarDisclaimer.text = result.disclaimerText

            // Re-render calendar cells
            generateCalendarDays(currentMonthCalendar)
        }

        // Observe Cycle History and update RecyclerView
        cycleViewModel.cycleHistory.observe(this) { historyList ->
            val count = historyList?.size ?: 0
            binding.tvCycleDataCount.text = "$count Siklus Data"

            if (historyList == null || historyList.isEmpty()) {
                binding.tvEmptyHistory.visibility = View.VISIBLE
                binding.rvCycleHistory.visibility = View.GONE
            } else {
                binding.tvEmptyHistory.visibility = View.GONE
                binding.rvCycleHistory.visibility = View.VISIBLE
                historyAdapter.updateData(historyList)
            }
        }

        // Setup Month Navigation Chevrons
        binding.btnPrevMonth.setOnClickListener {
            currentMonthCalendar.add(Calendar.MONTH, -1)
            generateCalendarDays(currentMonthCalendar)
        }

        binding.btnNextMonth.setOnClickListener {
            currentMonthCalendar.add(Calendar.MONTH, 1)
            generateCalendarDays(currentMonthCalendar)
        }

        // Buka panel tracking
        binding.btnCycleTracking.setOnClickListener {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        // Navigasi Tanggal di Bottom Sheet
        binding.btnPrevDate.setOnClickListener {
            val prev = currentSelectedCalendar.clone() as Calendar
            prev.add(Calendar.DAY_OF_YEAR, -1)
            setDate(prev)
        }

        // Navigasi Tanggal di Bottom Sheet
        binding.btnNextDate.setOnClickListener {
            val next = currentSelectedCalendar.clone() as Calendar
            next.add(Calendar.DAY_OF_YEAR, 1)
            setDate(next)
        }

        // Setup Flow selectors
        binding.btnHadFlow.setOnClickListener {
            setFlowSelected(true, "Medium")
            if (isTutorialMode) {
                updateTutorialGuideText()
            } else {
                saveLogAndPredictions(shouldClose = false)
            }
        }

        binding.btnNoFlow.setOnClickListener {
            setFlowSelected(false, "None")
            if (isTutorialMode) {
                updateTutorialGuideText()
            } else {
                saveLogAndPredictions(shouldClose = false)
            }
        }

        // Flow Level
        binding.flowLight.setOnClickListener { 
            setFlowSelected(true, "Light") 
            if (isTutorialMode) {
                updateTutorialGuideText()
            } else {
                saveLogAndPredictions(shouldClose = false)
            }
        }
        binding.flowMedium.setOnClickListener { 
            setFlowSelected(true, "Medium") 
            if (isTutorialMode) {
                updateTutorialGuideText()
            } else {
                saveLogAndPredictions(shouldClose = false)
            }
        }
        binding.flowHeavy.setOnClickListener { 
            setFlowSelected(true, "Heavy") 
            if (isTutorialMode) {
                updateTutorialGuideText()
            } else {
                saveLogAndPredictions(shouldClose = false)
            }
        }

        // Selesai & Simpan Log
        binding.btnDoneTracking.setOnClickListener {
            saveLogAndPredictions(shouldClose = true)
        }

        // Auto-save when symptom chips are clicked
        val symptomChipsList = listOf(
            binding.chipCramp, binding.chipLowerBack, binding.chipHairLoss, binding.chipAcne,
            binding.chipBloating, binding.chipHeadache, binding.chipMood, binding.chipFatigue,
            binding.chipBreast, binding.chipCravings
        )
        symptomChipsList.forEach { chip ->
            chip.setOnClickListener {
                if (isTutorialMode) {
                    updateTutorialGuideText()
                } else {
                    saveLogAndPredictions(shouldClose = false)
                }
            }
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked && !isBinding) {
                    playBoingSound()
                }
            }
        }
    }

    private fun setupPregnancyUI() {
        binding.tvHeaderTitle.text = "Kalender Kehamilan"
        binding.tvPredictionTitle.text = "Informasi Kehamilan"
        binding.tvBsCycleTitle.visibility = View.GONE
        binding.btnHadFlow.visibility = View.GONE
        binding.btnNoFlow.visibility = View.GONE
        binding.tvFlowLevelLabel.visibility = View.GONE
        binding.scrollFlowLevel.visibility = View.GONE
        binding.tvBsSymptomsTitle.text = "Gejala Kehamilan"
        binding.btnCycleTracking.text = "Log Fase & Gejala"
        binding.tvAvgCycleLabel.text = "Fase Kehamilan"
        binding.tvNextPeriodLabel.text = "Perkiraan Lahir (HPL)"

        // Rename chips to pregnancy symptoms
        binding.chipCramp.text = "Kram Ringan"
        binding.chipLowerBack.text = "Nyeri Punggung"
        binding.chipHairLoss.text = "Mual (Morning Sickness)"
        binding.chipAcne.text = "Pusing / Sakit Kepala"
        binding.chipBloating.text = "Perut Kembung"
        binding.chipHeadache.text = "Sembelit"
        binding.chipMood.text = "Sensitif / Mood Swing"
        binding.chipFatigue.text = "Sangat Lelah"
        binding.chipBreast.text = "Payudara Sensitif"
        binding.chipCravings.text = "Ngidam Makanan"

        // Set purple theme colors
        binding.header.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_light))
        binding.btnCycleTracking.backgroundTintList = ContextCompat.getColorStateList(this, R.color.purple)
        binding.tvCycleDataCount.setTextColor(ContextCompat.getColor(this, R.color.purple))
        binding.tvCycleDataCount.visibility = View.GONE

        binding.layoutTrimesterLegend.visibility = View.VISIBLE
        binding.cardColorLegend.visibility = View.GONE
        binding.cardCycleHistory.visibility = View.GONE
    }

    private fun setDate(calendar: Calendar) {
        currentSelectedCalendar = calendar

        // Sync month view if selection is in a different month
        val sameMonth = currentMonthCalendar.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                currentMonthCalendar.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)
        
        if (!sameMonth) {
            currentMonthCalendar.time = calendar.time
            currentMonthCalendar.set(Calendar.DAY_OF_MONTH, 1)
        }

        val displayFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        binding.tvSelectedDate.text = displayFormat.format(calendar.time)

        loadLogForSelectedDate()
        generateCalendarDays(currentMonthCalendar)
    }

    private fun getDayStart(timeMs: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timeMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun generateCalendarDays(monthCal: Calendar) {
        calendarGenerateJob?.cancel()

        calendarGenerateJob = lifecycleScope.launch {
            val daysList = mutableListOf<CalendarDay>()

            // Clone calendar and set to first day of the month
            val cal = monthCal.clone() as Calendar
            cal.set(Calendar.DAY_OF_MONTH, 1)

            val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val paddingCount = firstDayOfWeek - 1

            for (i in 0 until paddingCount) {
                daysList.add(CalendarDay(null, ""))
            }

            val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val today = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

            // Ambil semua log untuk bulan ini dari database
            val allLogsList = mutableMapOf<String, DailyLogEntity>()
            
            // Get prediction values from ViewModel
            val pred = cycleViewModel.predictionResult.value
            val nextPeriodStart = pred?.nextPeriodDate ?: 0L
            val rangeStart = pred?.predictionRangeStart ?: 0L
            val rangeEnd = pred?.predictionRangeEnd ?: 0L
            val fertileStart = pred?.fertileStart ?: 0L
            val fertileEnd = pred?.fertileEnd ?: 0L
            val hasPred = pred?.hasPrediction ?: false

            database.dailyLogDao().getAllLogs().collect { logs ->
                logs.forEach { log ->
                    allLogsList[log.date] = log
                }

                // Dapatkan LMP kehamilan
                var lmpCal: Calendar? = null
                if (isPregnant) {
                    val latestCycle = database.cycleDao().getLatestCycle()
                    if (latestCycle != null) {
                        val startParts = latestCycle.startDate.split("-")
                        if (startParts.size == 3) {
                            lmpCal = Calendar.getInstance().apply {
                                set(startParts[0].toInt(), startParts[1].toInt() - 1, startParts[2].toInt())
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                        }
                    }
                }

                for (day in 1..totalDays) {
                    val dayCal = cal.clone() as Calendar
                    dayCal.set(Calendar.DAY_OF_MONTH, day)
                    // Reset time to midnight for accurate difference calculation
                    dayCal.set(Calendar.HOUR_OF_DAY, 0)
                    dayCal.set(Calendar.MINUTE, 0)
                    dayCal.set(Calendar.SECOND, 0)
                    dayCal.set(Calendar.MILLISECOND, 0)

                    val dateKey = sdf.format(dayCal.time)

                    val isSel = sdf.format(currentSelectedCalendar.time) == dateKey
                    val isTod = sdf.format(today.time) == dateKey

                    val log = allLogsList[dateKey]
                    val hasL = log != null && (!log.symptoms.isNullOrEmpty() || (log.flowLevel != null && log.flowLevel != "None"))
                    val flow = log?.flowLevel

                    val dayMs = dayCal.timeInMillis

                    var isPredPeriod = false
                    if (!isPregnant && hasPred && rangeStart > 0L) {
                        val dayStart = getDayStart(dayMs)
                        val rStart = getDayStart(rangeStart)
                        val rEnd = getDayStart(rangeEnd)
                        if (dayStart in rStart..rEnd) {
                            isPredPeriod = true
                        }
                    }

                    var isPredFertile = false
                    if (!isPregnant && hasPred && fertileStart > 0L) {
                        val dayStart = getDayStart(dayMs)
                        val fStart = getDayStart(fertileStart)
                        val fEnd = getDayStart(fertileEnd)
                        if (dayStart in fStart..fEnd) {
                            isPredFertile = true
                        }
                    }

                    var isPredOv = false
                    if (!isPregnant && hasPred && nextPeriodStart > 0L) {
                        val dayStart = getDayStart(dayMs)
                        val ovDate = nextPeriodStart - 14L * 24 * 60 * 60 * 1000
                        val ovStart = getDayStart(ovDate)
                        if (dayStart == ovStart) {
                            isPredOv = true
                        }
                    }

                    var pregWeek: Int? = null
                    var trimester: Int? = null

                    if (isPregnant && lmpCal != null) {
                        val diffMs = dayCal.timeInMillis - lmpCal.timeInMillis
                        val dayOffset = (diffMs / (1000 * 60 * 60 * 24)).toInt()
                        if (dayOffset >= 0) {
                            val week = dayOffset / 7
                            pregWeek = week
                            trimester = when {
                                week <= 12 -> 1
                                week <= 27 -> 2
                                else -> 3
                            }
                        }
                    }

                    daysList.add(
                        CalendarDay(
                            date = dayCal.time,
                            dayNumber = day.toString(),
                            isSelected = isSel,
                            isToday = isTod,
                            hasLog = hasL,
                            flowLevel = flow,
                            isPredictedPeriod = isPredPeriod,
                            isPredictedFertile = isPredFertile,
                            isPredictedOvulation = isPredOv,
                            pregnancyWeek = pregWeek,
                            trimester = trimester
                        )
                    )
                }

                while (daysList.size % 7 != 0) {
                    daysList.add(CalendarDay(null, ""))
                }

                val headerFormat = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
                if (isPregnant) {
                    binding.tvHeaderSub.text = "Pantau Usia & Perkembangan Kehamilan · ${headerFormat.format(monthCal.time)}"
                } else if (pred != null && pred.hasPrediction) {
                    binding.tvHeaderSub.text = "Prediksi Siklus Pintar · ${headerFormat.format(monthCal.time)}"
                } else {
                    binding.tvHeaderSub.text = "Prediksi Siklus Pintar · ${headerFormat.format(monthCal.time)}"
                }

                setupCalendarGrid(daysList)
            }
        }
    }

    private fun setupCalendarGrid(days: List<CalendarDay>) {
        val adapter = CalendarGridAdapter(days) { day ->
            if (day.date != null) {
                val cal = Calendar.getInstance()
                cal.time = day.date
                setDate(cal)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        binding.rvCalendarGrid.adapter = adapter
    }

    private fun loadLogForSelectedDate() {
        logCollectJob?.cancel()

        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(currentSelectedCalendar.time)

        logCollectJob = lifecycleScope.launch {
            database.dailyLogDao().getLogByDate(dateStr).collect { log ->
                isBinding = true
                if (log != null) {
                    currentLogId = log.id
                    if (log.flowLevel != null && log.flowLevel != "None") {
                        setFlowSelected(true, log.flowLevel)
                    } else {
                        setFlowSelected(false, "None")
                    }

                    val symptoms = log.symptoms?.split(", ") ?: emptyList()
                    setSymptomChipsChecked(symptoms)
                } else {
                    currentLogId = 0
                    resetSelections()
                }
                isBinding = false
            }
        }
    }

    private fun setFlowSelected(hadFlow: Boolean, level: String) {
        if (hadFlow) {
            currentFlowLevel = level
            binding.btnHadFlow.setBackgroundResource(R.drawable.bg_pill_pink)
            binding.btnHadFlow.setTextColor(ContextCompat.getColor(this, R.color.p))

            binding.btnNoFlow.setBackgroundResource(R.drawable.bg_quick_card)
            binding.btnNoFlow.setTextColor(ContextCompat.getColor(this, R.color.pm))

            binding.tvFlowLevelLabel.visibility = View.VISIBLE
            binding.scrollFlowLevel.visibility = View.VISIBLE

            binding.flowLight.setBackgroundResource(if (currentFlowLevel == "Light") R.drawable.bg_pill_pink else R.drawable.bg_quick_card)
            binding.flowLight.setTextColor(ContextCompat.getColor(this, if (currentFlowLevel == "Light") R.color.p else R.color.pm))

            binding.flowMedium.setBackgroundResource(if (currentFlowLevel == "Medium") R.drawable.bg_pill_pink else R.drawable.bg_quick_card)
            binding.flowMedium.setTextColor(ContextCompat.getColor(this, if (currentFlowLevel == "Medium") R.color.p else R.color.pm))

            binding.flowHeavy.setBackgroundResource(if (currentFlowLevel == "Heavy") R.drawable.bg_pill_pink else R.drawable.bg_quick_card)
            binding.flowHeavy.setTextColor(ContextCompat.getColor(this, if (currentFlowLevel == "Heavy") R.color.p else R.color.pm))
        } else {
            currentFlowLevel = "None"
            binding.btnHadFlow.setBackgroundResource(R.drawable.bg_quick_card)
            binding.btnHadFlow.setTextColor(ContextCompat.getColor(this, R.color.pm))

            binding.btnNoFlow.setBackgroundResource(R.drawable.bg_pill_pink)
            binding.btnNoFlow.setTextColor(ContextCompat.getColor(this, R.color.p))

            binding.tvFlowLevelLabel.visibility = View.GONE
            binding.scrollFlowLevel.visibility = View.GONE
        }
    }

    private fun setSymptomChipsChecked(symptoms: List<String>) {
        val chips = listOf(
            binding.chipCramp, binding.chipLowerBack, binding.chipHairLoss, binding.chipAcne,
            binding.chipBloating, binding.chipHeadache, binding.chipMood, binding.chipFatigue,
            binding.chipBreast, binding.chipCravings
        )
        chips.forEach { chip ->
            chip.isChecked = symptoms.contains(chip.text.toString())
        }
    }

    private fun resetSelections() {
        val wasBinding = isBinding
        isBinding = true
        currentFlowLevel = "None"
        binding.btnHadFlow.setBackgroundResource(R.drawable.bg_quick_card)
        binding.btnHadFlow.setTextColor(ContextCompat.getColor(this, R.color.pm))

        binding.btnNoFlow.setBackgroundResource(R.drawable.bg_quick_card)
        binding.btnNoFlow.setTextColor(ContextCompat.getColor(this, R.color.pm))

        binding.tvFlowLevelLabel.visibility = View.GONE
        binding.scrollFlowLevel.visibility = View.GONE

        val chips = listOf(
            binding.chipCramp, binding.chipLowerBack, binding.chipHairLoss, binding.chipAcne,
            binding.chipBloating, binding.chipHeadache, binding.chipMood, binding.chipFatigue,
            binding.chipBreast, binding.chipCravings
        )
        chips.forEach { it.isChecked = false }
        isBinding = wasBinding
    }

    private fun updateTutorialGuideText() {
        if (!isTutorialMode) return
        
        // Hide the top card completely
        binding.cardCalendarTutorialGuide.visibility = View.GONE
        
        // Reset highlights first
        val btnTracking = binding.btnCycleTracking as? com.google.android.material.button.MaterialButton
        btnTracking?.strokeWidth = 0
        
        val doneBtn = binding.btnDoneTracking as? com.google.android.material.button.MaterialButton
        doneBtn?.strokeWidth = 0
        
        binding.chipCramp.chipStrokeWidth = 0f
        binding.chipHairLoss.chipStrokeWidth = 0f
        
        if (behavior.state != BottomSheetBehavior.STATE_EXPANDED) {
            binding.tvTutorialTrackingHint.visibility = View.VISIBLE
            binding.tvTutorialHadFlowHint.visibility = View.GONE
            binding.tvTutorialFlowLevelHint.visibility = View.GONE
            binding.tvTutorialSymptomsHint.visibility = View.GONE
            binding.tvTutorialSaveHint.visibility = View.GONE
            
            // Highlight tracking button
            btnTracking?.strokeColor = ContextCompat.getColorStateList(this, R.color.gold)
            btnTracking?.strokeWidth = 6
            
            // Re-render calendar grid to show today highlighted
            binding.rvCalendarGrid.adapter?.notifyDataSetChanged()
        } else {
            binding.tvTutorialTrackingHint.visibility = View.GONE
            
            val selectedSymptoms = mutableListOf<String>()
            val chips = listOf(
                binding.chipCramp, binding.chipLowerBack, binding.chipHairLoss, binding.chipAcne,
                binding.chipBloating, binding.chipHeadache, binding.chipMood, binding.chipFatigue,
                binding.chipBreast, binding.chipCravings
            )
            chips.forEach {
                if (it.isChecked) {
                    selectedSymptoms.add(it.text.toString())
                }
            }
            
            // Re-render calendar grid (remove highlights if any)
            binding.rvCalendarGrid.adapter?.notifyDataSetChanged()
            
            if (isPregTutorialMode) {
                // In pregnancy tutorial mode, no flow is needed
                binding.tvTutorialHadFlowHint.visibility = View.GONE
                binding.tvTutorialFlowLevelHint.visibility = View.GONE
                
                if (selectedSymptoms.isEmpty()) {
                    binding.tvTutorialSymptomsHint.text = "👇 Langkah 1: Pilih minimal 1 gejala fisik kehamilan Anda"
                    binding.tvTutorialSymptomsHint.visibility = View.VISIBLE
                    binding.tvTutorialSaveHint.visibility = View.GONE
                    
                    // Highlight chipHairLoss (Mual) for guidance
                    binding.chipHairLoss.chipStrokeColor = ContextCompat.getColorStateList(this, R.color.gold)
                    binding.chipHairLoss.chipStrokeWidth = 6f
                } else {
                    binding.tvTutorialSymptomsHint.visibility = View.GONE
                    binding.tvTutorialSaveHint.text = "👇 Langkah 2: Klik Simpan Catatan untuk menyimpan log"
                    binding.tvTutorialSaveHint.visibility = View.VISIBLE
                    
                    // Highlight Selesai & Simpan
                    doneBtn?.strokeColor = ContextCompat.getColorStateList(this, R.color.gold)
                    doneBtn?.strokeWidth = 6
                }
            } else {
                val isHadFlow = currentFlowLevel != "None"
                if (!isHadFlow) {
                    binding.tvTutorialHadFlowHint.visibility = View.VISIBLE
                    binding.tvTutorialFlowLevelHint.visibility = View.GONE
                    binding.tvTutorialSymptomsHint.visibility = View.GONE
                    binding.tvTutorialSaveHint.visibility = View.GONE
                    
                    // Highlight 'Ada Haid' button
                    binding.btnHadFlow.setBackgroundResource(R.drawable.bg_tutorial_highlight)
                } else {
                    // Restore button backgrounds
                    setFlowSelected(true, currentFlowLevel)
                    binding.tvTutorialHadFlowHint.visibility = View.GONE
                    
                    if (selectedSymptoms.isEmpty()) {
                        binding.tvTutorialFlowLevelHint.visibility = View.VISIBLE
                        binding.tvTutorialSymptomsHint.visibility = View.VISIBLE
                        binding.tvTutorialSaveHint.visibility = View.GONE
                        
                        // Highlight chipCramp as a guidance
                        binding.chipCramp.chipStrokeColor = ContextCompat.getColorStateList(this, R.color.gold)
                        binding.chipCramp.chipStrokeWidth = 6f
                    } else {
                        binding.tvTutorialFlowLevelHint.visibility = View.GONE
                        binding.tvTutorialSymptomsHint.visibility = View.GONE
                        binding.tvTutorialSaveHint.visibility = View.VISIBLE
                        
                        // Highlight Selesai & Simpan
                        doneBtn?.strokeColor = ContextCompat.getColorStateList(this, R.color.gold)
                        doneBtn?.strokeWidth = 6
                    }
                }
            }
        }
    }

    private fun saveLogAndPredictions(shouldClose: Boolean = true) {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(currentSelectedCalendar.time)

        val selectedSymptoms = mutableListOf<String>()
        val chips = listOf(
            binding.chipCramp, binding.chipLowerBack, binding.chipHairLoss, binding.chipAcne,
            binding.chipBloating, binding.chipHeadache, binding.chipMood, binding.chipFatigue,
            binding.chipBreast, binding.chipCravings
        )
        chips.forEach {
            if (it.isChecked) {
                selectedSymptoms.add(it.text.toString())
            }
        }
        val symptomsString = selectedSymptoms.joinToString(", ")

        if (isTutorialMode && shouldClose) {
            if (isPregTutorialMode) {
                if (selectedSymptoms.isEmpty()) {
                    Toast.makeText(this, "Harap pilih minimal 1 gejala fisik untuk merekam kondisi Anda.", Toast.LENGTH_SHORT).show()
                    return
                }
            } else {
                if (currentFlowLevel == "None") {
                    Toast.makeText(this, "Harap tandai 'Ada Haid' untuk merekam siklus Anda.", Toast.LENGTH_SHORT).show()
                    return
                }
                if (selectedSymptoms.isEmpty()) {
                    Toast.makeText(this, "Harap pilih minimal 1 gejala fisik untuk merekam siklus Anda.", Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }

        val flowVal = if (currentFlowLevel == "None") null else currentFlowLevel
        val noteText = "Flow: $currentFlowLevel, Symptoms: $symptomsString"
        val encryptedNote = EncryptionManager.encrypt(noteText)

        val newLog = DailyLogEntity(
            id = currentLogId,
            date = dateStr,
            flowLevel = flowVal,
            symptoms = if (symptomsString.isNotEmpty()) symptomsString else null,
            encryptedNote = encryptedNote,
            phase = "CALCULATED"
        )

        lifecycleScope.launch {
            try {
                database.dailyLogDao().insertLog(newLog)
                if (shouldClose) {
                    Toast.makeText(this@CalendarActivity, "Catatan berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    behavior.state = BottomSheetBehavior.STATE_HIDDEN
                }
                
                // Recalculate predictions and history
                cycleViewModel.loadPredictionData(isPregnant)

                if (shouldClose && isTutorialMode) {
                    val prefs = getSharedPreferences("cycle_sync_prefs", MODE_PRIVATE)
                    if (isPregTutorialMode) {
                        prefs.edit().putInt("pregnancy_onboarding_step", 2).apply()
                    } else {
                        prefs.edit().putInt("onboarding_step", 2).apply()
                    }
                    finish()
                }
            } catch (e: Exception) {
                if (shouldClose) {
                    Toast.makeText(this@CalendarActivity, "Gagal menyimpan catatan", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun playBoingSound() {
        lifecycleScope.launch(Dispatchers.Default) {
            val sampleRate = 22050
            val duration = 0.35 // seconds
            val numSamples = (duration * sampleRate).toInt()
            val buffer = ShortArray(numSamples)

            val bounce1Duration = 0.18
            val bounce2Duration = 0.17

            for (i in 0 until numSamples) {
                val time = i.toDouble() / sampleRate
                var mixed = 0.0
                
                if (time < bounce1Duration) {
                    // First bounce: sweep from 150 Hz to 450 Hz
                    val t = time
                    val f0 = 150.0
                    val f1 = 450.0
                    val phase = 2.0 * Math.PI * (f0 * t + (f1 - f0) * t * t / (2.0 * bounce1Duration))
                    val env = (1.0 - t / bounce1Duration).coerceIn(0.0, 1.0)
                    mixed = Math.sin(phase) * env
                } else {
                    // Second bounce: sweep from 130 Hz to 380 Hz
                    val t = time - bounce1Duration
                    val f0 = 130.0
                    val f1 = 380.0
                    val phase = 2.0 * Math.PI * (f0 * t + (f1 - f0) * t * t / (2.0 * bounce2Duration))
                    val env = 0.75 * (1.0 - t / bounce2Duration).coerceIn(0.0, 1.0)
                    mixed = Math.sin(phase) * env
                }
                
                buffer[i] = (mixed * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
            }

            withContext(Dispatchers.Main) {
                try {
                    val audioTrack = AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        buffer.size * 2,
                        AudioTrack.MODE_STATIC
                    )
                    audioTrack.write(buffer, 0, buffer.size)
                    audioTrack.play()
                    
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        try {
                            audioTrack.stop()
                            audioTrack.release()
                        } catch (e: Exception) {}
                    }, (duration * 1000 + 200).toLong())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    inner class CalendarGridAdapter(
        private val days: List<CalendarDay>,
        private val onDayClick: (CalendarDay) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<CalendarGridAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            val viewHighlight: View = view.findViewById(R.id.viewHighlight)
            val tvDayNumber: TextView = view.findViewById(R.id.tvDayNumber)
            val tvTutorialHint: TextView = view.findViewById(R.id.tvTutorialHint)
            val viewLogDot: View = view.findViewById(R.id.viewLogDot)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val day = days[position]
            if (day.date == null) {
                holder.tvDayNumber.text = ""
                holder.viewHighlight.visibility = View.GONE
                holder.tvTutorialHint.visibility = View.GONE
                holder.viewLogDot.visibility = View.GONE
                holder.itemView.isClickable = false
            } else {
                holder.tvDayNumber.text = day.dayNumber
                holder.itemView.isClickable = true
                holder.itemView.setOnClickListener { onDayClick(day) }

                val isTodayInTutorial = isTutorialMode && day.isToday && behavior.state != BottomSheetBehavior.STATE_EXPANDED
                holder.tvTutorialHint.visibility = if (isTodayInTutorial) View.VISIBLE else View.GONE
                
                if (isTodayInTutorial) {
                    holder.itemView.setBackgroundResource(R.drawable.bg_tutorial_highlight)
                } else if (day.isSelected) {
                    holder.tvDayNumber.setTypeface(null, android.graphics.Typeface.BOLD)
                    holder.itemView.setBackgroundResource(R.drawable.bg_card_outline)
                } else {
                    holder.tvDayNumber.setTypeface(null, android.graphics.Typeface.NORMAL)
                    holder.itemView.background = null
                }

                // Dot indicator jika ada gejala/log tercatat
                holder.viewLogDot.visibility = if (day.hasLog) View.VISIBLE else View.GONE
                holder.viewLogDot.backgroundTintList = ContextCompat.getColorStateList(
                    holder.itemView.context,
                    if (isPregnant) R.color.purple else R.color.p
                )

                // Pewarnaan lingkaran berdasarkan status
                if (isPregnant && day.trimester != null) {
                    holder.viewHighlight.visibility = View.VISIBLE
                    when (day.trimester) {
                        1 -> {
                            holder.viewHighlight.setBackgroundResource(R.drawable.bg_circle_t1)
                            holder.tvDayNumber.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.pt))
                        }
                        2 -> {
                            holder.viewHighlight.setBackgroundResource(R.drawable.bg_circle_t2)
                            holder.tvDayNumber.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.pt))
                        }
                        3 -> {
                            holder.viewHighlight.setBackgroundResource(R.drawable.bg_circle_t3)
                            holder.tvDayNumber.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
                        }
                    }
                } else {
                    when {
                        // 1. Hari Haid Aktual (warna pink solid/red solid sesuai flow)
                        day.flowLevel != null && day.flowLevel != "None" -> {
                            holder.viewHighlight.visibility = View.VISIBLE
                            when (day.flowLevel) {
                                "Light" -> holder.viewHighlight.setBackgroundResource(R.drawable.bg_circle_period_light)
                                "Heavy" -> holder.viewHighlight.setBackgroundResource(R.drawable.bg_circle_period_heavy)
                                else -> holder.viewHighlight.setBackgroundResource(R.drawable.bg_circle_period_medium) // Medium
                            }
                            holder.tvDayNumber.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
                        }

                        // 2. Hari Ovulasi Terprediksi (lingkaran biru tua)
                        day.isPredictedOvulation -> {
                            holder.viewHighlight.visibility = View.VISIBLE
                            holder.viewHighlight.setBackgroundResource(R.drawable.bg_circle_ovulation)
                            holder.tvDayNumber.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.pt))
                        }

                        // 3. Jendela Masa Subur Terprediksi (lingkaran biru muda)
                        day.isPredictedFertile -> {
                            holder.viewHighlight.visibility = View.VISIBLE
                            holder.viewHighlight.setBackgroundResource(R.drawable.bg_circle_fertile)
                            holder.tvDayNumber.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.pt))
                        }

                        // 4. Hari Haid Terprediksi (lingkaran pink muda transparan)
                        day.isPredictedPeriod -> {
                            holder.viewHighlight.visibility = View.VISIBLE
                            holder.viewHighlight.setBackgroundResource(R.drawable.bg_circle_period_light)
                            holder.tvDayNumber.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.pt))
                        }

                        else -> {
                            holder.viewHighlight.visibility = View.GONE
                            holder.tvDayNumber.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.pt))
                        }
                    }
                }
            }
        }

        override fun getItemCount(): Int = days.size
    }

    companion object {
        private val fetalDevelopmentData = mapOf(
            0 to Pair("🌱 Sel Telur Terbuahi", "Pembuahan terjadi. Sel telur mulai membelah diri menjadi blastokista."),
            1 to Pair("✨ Implantasi", "Blastokista menempel pada dinding rahim. Proses implantasi dimulai."),
            2 to Pair("🎒 Kantung Kehamilan", "Kantung kehamilan terbentuk. Embrio berukuran mikroskopis."),
            3 to Pair("🌾 Sebutir Wijen", "Panjang embrio sekitar 1.5 mm. Sistem saraf dan jantung mulai terbentuk."),
            4 to Pair("🍏 Seukuran Biji Apel (sekitar 2 mm)", "Jantung mulai berdenyut dan memompa darah. Struktur dasar organ terbentuk."),
            5 to Pair("🍊 Seukuran Biji Jeruk (sekitar 3 mm)", "Kuncup tangan dan kaki mulai tumbuh. Tabung saraf menutup sempurna."),
            6 to Pair("🍇 Seukuran Biji Anggur (sekitar 5-6 mm)", "Wajah mulai terbentuk dengan titik gelap untuk mata dan hidung."),
            7 to Pair("🫐 Seukuran Blueberry (sekitar 10 mm)", "Otak berkembang pesat. Jari tangan dan kaki mulai terbentuk secara kasar."),
            8 to Pair("🍒 Seukuran Raspberry (sekitar 1.6 cm)", "Ekor embrio menghilang. Gerakan spontan pertama terjadi tapi belum terasa."),
            9 to Pair("🫒 Seukuran Zaitun (sekitar 2.3 cm)", "Kelopak mata terbentuk dan menutup. Sendi utama seperti siku mulai berfungsi."),
            10 to Pair("🍓 Seukuran Stroberi (sekitar 3.1 cm, 4 gr)", "Embrio resmi menjadi janin. Jari-jari terpisah sepenuhnya dan kuku mulai tumbuh."),
            11 to Pair("🍋 Seukuran Jeruk Nipis (sekitar 4.1 cm, 7 gr)", "Janin mulai bergerak aktif di dalam ketuban. Organ tubuh luar terbentuk."),
            12 to Pair("🍒 Seukuran Plum (sekitar 5.4 cm, 14 gr)", "Refleks janin berkembang. Ginjal mulai memproduksi urine."),
            13 to Pair("🍋 Seukuran Lemon (sekitar 7.4 cm, 23 gr)", "Sidik jari unik terbentuk. Janin bisa memasukkan jempol ke mulut."),
            14 to Pair("🍑 Seukuran Persik (sekitar 8.7 cm, 43 gr)", "Rambut halus (lanugo) menutupi tubuh. Leher memanjang dan kepala tegak."),
            15 to Pair("🍎 Seukuran Apel (sekitar 10 cm, 70 gr)", "Mata dan telinga berpindah ke posisi yang benar. Janin mulai bisa mendengar."),
            16 to Pair("🥑 Seukuran Alpukat (sekitar 11.6 cm, 100 gr)", "Kulit janin masih transparan. Jantung memompa sekitar 25 liter darah per hari."),
            17 to Pair("🧅 Seukuran Bawang Merah (sekitar 13 cm, 140 gr)", "Lapisan lemak pelindung mulai terbentuk di bawah kulit."),
            18 to Pair("🍠 Seukuran Ubi Jalar (sekitar 14.2 cm, 190 gr)", "Janin mulai bisa mendengar suara dari luar rahim. Ibu mulai merasakan gerakan halus."),
            19 to Pair("🥭 Seukuran Mangga (sekitar 15.3 cm, 240 gr)", "Lapisan vernix caseosa melindungi kulit dari cairan ketuban."),
            20 to Pair("🍌 Seukuran Pisang (sekitar 25.6 cm, 300 gr)", "Titik tengah kehamilan. Kelamin janin sudah terlihat jelas pada USG."),
            21 to Pair("🥕 Seukuran Wortel (sekitar 26.7 cm, 360 gr)", "Janin mulai menelan cairan ketuban untuk melatih sistem pencernaan."),
            22 to Pair("🥥 Seukuran Kelapa Muda (sekitar 27.8 cm, 430 gr)", "Indra peraba janin berkembang pesat. Janin bisa meraba wajahnya sendiri."),
            23 to Pair("🍇 Seukuran Grapefruit (sekitar 28.9 cm, 500 gr)", "Pendengaran janin semakin tajam. Paru-paru memproduksi surfaktan."),
            24 to Pair("🌽 Seukuran Jagung (sekitar 30 cm, 600 gr)", "Siklus tidur-bangun janin mulai terbentuk secara teratur."),
            25 to Pair("🥒 Seukuran Mentimun (sekitar 34.6 cm, 660 gr)", "Struktur tulang semakin kuat. Kulit janin mulai berisi."),
            26 to Pair("🥬 Seukuran Selada (sekitar 35.6 cm, 760 gr)", "Mata janin mulai terbuka untuk pertama kalinya."),
            27 to Pair("🥦 Seukuran Kembang Kol (sekitar 36.6 cm, 875 gr)", "Janin mulai bisa mengenali suara Ibu secara jelas."),
            28 to Pair("🍆 Seukuran Terong (sekitar 37.6 cm, 1 kg)", "Trimester 3 dimulai. Otak janin berkembang sangat pesat."),
            29 to Pair("🍍 Seukuran Nanas (sekitar 38.6 cm, 1.2 kg)", "Janin dapat membedakan cahaya terang dan gelap melalui dinding rahim."),
            30 to Pair("🥬 Seukuran Kubis (sekitar 39.9 cm, 1.3 kg)", "Volume air ketuban berkurang seiring pertumbuhan janin."),
            31 to Pair("🥥 Seukuran Kelapa Tua (sekitar 41.1 cm, 1.5 kg)", "Janin dapat menolehkan kepala dari sisi ke sisi."),
            32 to Pair("🍍 Seukuran Nanas Madu (sekitar 42.4 cm, 1.7 kg)", "Semua panca indra janin berfungsi penuh."),
            33 to Pair("🍈 Seukuran Melon (sekitar 43.7 cm, 1.9 kg)", "Sistem imun janin diperkuat oleh antibodi dari Ibu."),
            34 to Pair("🍈 Seukuran Blewah (sekitar 45 cm, 2.1 kg)", "Paru-paru janin sudah hampir matang sempurna."),
            35 to Pair("🍉 Seukuran Semangka Kecil (sekitar 46.2 cm, 2.4 kg)", "Sebagian besar organ tubuh janin sudah berfungsi mandiri."),
            36 to Pair("🥬 Seukuran Selada Romaine (sekitar 47.4 cm, 2.6 kg)", "Janin mulai turun ke rongga panggul."),
            37 to Pair("🍈 Seukuran Labu Parang (sekitar 48.6 cm, 2.9 kg)", "Kehamilan sudah dianggap cukup bulan (full term)."),
            38 to Pair("🍉 Seukuran Semangka Sedang (sekitar 49.8 cm, 3.1 kg)", "Vernix caseosa mulai luruh. Refleks hisap janin sangat kuat."),
            39 to Pair("🍉 Seukuran Semangka Besar (sekitar 50.7 cm, 3.3 kg)", "Tali pusat mengalirkan antibodi. Bayi siap dilahirkan."),
            40 to Pair("👶 Bayi Siap Lahir! (sekitar 51 cm, 3.4 kg)", "Hari perkiraan lahir Anda. Selamat menyambut buah hati tercinta!")
        )
    }

    inner class CycleHistoryAdapter(
        private var items: List<CycleEntity> = emptyList()
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<CycleHistoryAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            val viewCircle: View = view.findViewById(R.id.viewCircle)
            val tvHistoryStartDate: TextView = view.findViewById(R.id.tvHistoryStartDate)
            val tvHistoryNotes: TextView = view.findViewById(R.id.tvHistoryNotes)
            val tvHistoryCycleLength: TextView = view.findViewById(R.id.tvHistoryCycleLength)
        }

        fun updateData(newItems: List<CycleEntity>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_cycle_history, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            
            // Format start date beautifully (e.g. 12 Feb 2026)
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val formatter = SimpleDateFormat("d MMM yyyy", Locale("id", "ID"))
            val formattedDate = try {
                parser.parse(item.startDate)?.let { formatter.format(it) } ?: item.startDate
            } catch (e: Exception) {
                item.startDate
            }
            
            holder.tvHistoryStartDate.text = formattedDate
            holder.tvHistoryNotes.text = item.notes ?: "Tidak ada catatan"
            holder.tvHistoryCycleLength.text = "${item.cycleLength} hari"
            
            holder.viewCircle.setBackgroundResource(R.drawable.bg_circle_period_medium)
        }

        override fun getItemCount(): Int = items.size
    }
}
