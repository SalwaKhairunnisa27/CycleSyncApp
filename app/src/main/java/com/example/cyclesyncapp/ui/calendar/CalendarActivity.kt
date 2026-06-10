package com.example.cyclesyncapp.ui.calendar

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
import com.example.cyclesyncapp.databinding.ActivityCalendarBinding
import com.example.cyclesyncapp.ui.viewmodel.CycleViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
    val isPredictedOvulation: Boolean = false
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
        isTutorialMode = onboardingStep == 1
        if (isTutorialMode) {
            binding.cardCalendarTutorialGuide.visibility = View.VISIBLE
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
                        }
                    } else {
                        binding.tvAvgCycle.text = "--"
                        binding.tvNextPeriod.text = "--"
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
            
            binding.tvAvgCycle.text = "${result.averageCycle} hr"
            
            if (result.hasPrediction && result.nextPeriodDate > 0L) {
                val outputFormat = SimpleDateFormat("d MMM", Locale("id", "ID"))
                
                // Show prediction range e.g. "12 Feb - 16 Feb"
                val rangeStartCal = Calendar.getInstance().apply { timeInMillis = result.predictionRangeStart }
                val rangeEndCal = Calendar.getInstance().apply { timeInMillis = result.predictionRangeEnd }
                val rangeStr = "${outputFormat.format(rangeStartCal.time)} - ${outputFormat.format(rangeEndCal.time)}"
                binding.tvNextPeriod.text = rangeStr
                
                binding.tvHeaderSub.text = "Prediksi Siklus Pintar · ${SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(currentMonthCalendar.time)}"
            } else {
                binding.tvNextPeriod.text = "--"
                binding.tvHeaderSub.text = "Belum ada data haid yang tercatat"
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
            saveLogAndPredictions(shouldClose = false)
        }

        binding.btnNoFlow.setOnClickListener {
            setFlowSelected(false, "None")
            saveLogAndPredictions(shouldClose = false)
        }

        // Flow Level
        binding.flowLight.setOnClickListener { 
            setFlowSelected(true, "Light") 
            saveLogAndPredictions(shouldClose = false)
        }
        binding.flowMedium.setOnClickListener { 
            setFlowSelected(true, "Medium") 
            saveLogAndPredictions(shouldClose = false)
        }
        binding.flowHeavy.setOnClickListener { 
            setFlowSelected(true, "Heavy") 
            saveLogAndPredictions(shouldClose = false)
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
                saveLogAndPredictions(shouldClose = false)
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

                for (day in 1..totalDays) {
                    val dayCal = cal.clone() as Calendar
                    dayCal.set(Calendar.DAY_OF_MONTH, day)
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
                            isPredictedOvulation = isPredOv
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
                    binding.tvHeaderSub.text = "Belum ada data haid yang tercatat"
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
                    prefs.edit().putInt("onboarding_step", 2).apply()
                    finish()
                }
            } catch (e: Exception) {
                if (shouldClose) {
                    Toast.makeText(this@CalendarActivity, "Gagal menyimpan catatan", Toast.LENGTH_SHORT).show()
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
                holder.viewLogDot.visibility = View.GONE
                holder.itemView.isClickable = false
            } else {
                holder.tvDayNumber.text = day.dayNumber
                holder.itemView.isClickable = true
                holder.itemView.setOnClickListener { onDayClick(day) }

                if (day.isSelected) {
                    holder.tvDayNumber.setTypeface(null, android.graphics.Typeface.BOLD)
                    holder.itemView.setBackgroundResource(R.drawable.bg_card_outline)
                } else {
                    holder.tvDayNumber.setTypeface(null, android.graphics.Typeface.NORMAL)
                    holder.itemView.background = null
                }

                // Dot indicator jika ada gejala/log tercatat
                holder.viewLogDot.visibility = if (day.hasLog) View.VISIBLE else View.GONE

                // Pewarnaan lingkaran berdasarkan status
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

        override fun getItemCount(): Int = days.size
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
