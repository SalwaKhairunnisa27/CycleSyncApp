package com.example.cyclesyncapp.ui.dashboard

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.cyclesyncapp.R
import com.example.cyclesyncapp.data.local.database.CycleDatabase
import com.example.cyclesyncapp.data.local.entity.CycleEntity
import com.example.cyclesyncapp.data.repository.UserRepositoryImpl
import com.example.cyclesyncapp.databinding.ActivityDashboardBinding
import com.example.cyclesyncapp.data.local.notification.NotificationScheduler
import com.example.cyclesyncapp.domain.usecase.PartnerSyncUseCase
import com.example.cyclesyncapp.ui.adapter.RecommendationAdapter
import com.example.cyclesyncapp.ui.calendar.CalendarActivity
import com.example.cyclesyncapp.ui.viewmodel.CycleViewModel
import com.example.cyclesyncapp.ui.viewmodel.RecommendationViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.exp
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.util.Calendar
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

    private val recommendationViewModel: RecommendationViewModel by viewModels()
    private val cycleViewModel: CycleViewModel by viewModels()
    private val recommendationAdapter = RecommendationAdapter()
    private val partnerSyncUseCase = PartnerSyncUseCase()
    private lateinit var notificationScheduler: NotificationScheduler
    private lateinit var userRepository: UserRepositoryImpl
    private lateinit var database: CycleDatabase
    private var isUserPregnant: Boolean = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission result if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Pre-initialize postpartum TTS engine early to avoid clicking latency
        PostpartumActivity.preInitialize(this)

        database = CycleDatabase.getDatabase(this)
        userRepository = UserRepositoryImpl(database.userDao())
        notificationScheduler = NotificationScheduler(this)
        requestNotificationPermission()

        setupUI()
        setupRecyclerView()
        setupObservers()
        checkUserMode()
        loadUserCycleData(database)

        // Ambil data awal rekomendasi
        recommendationViewModel.loadRecommendations("LUTEAL")

        // Setup interactive onboarding tutorial
        setupTutorial()
    }

    private fun loadUserCycleData(database: CycleDatabase) {
        lifecycleScope.launch {
            // Get active user email from SharedPreferences
            val prefs = getSharedPreferences("cycle_sync_prefs", MODE_PRIVATE)
            val activeEmail = prefs.getString("active_user_email", "aisyah@email.com") ?: "aisyah@email.com"
            val user = userRepository.getUserByEmail(activeEmail) ?: userRepository.getUser()
            val isPregnant = user?.isPregnant ?: false

            cycleViewModel.loadPredictionData(isPregnant)
        }
    }

    private fun showNoPredictionsOnDashboard() {
        binding.tvPhaseBadge.text = "Siklus Belum Tercatat"
        binding.tvCycleDayValue.text = "--"
        binding.tvCycleDayLabel.text = "Silakan catat haid Anda"
        binding.cycleRing.progress = 0
        
        binding.tvStat1Value.text = "--"
        binding.tvStat2Value.text = "--"
        binding.tvStat3Value.text = "--"
        
        binding.tvNextPeriodPrediction.text = "Prediksi haid: silakan catat haid terlebih dahulu"
        binding.tvFertileWindowPrediction.text = "Prediksi masa subur: silakan catat haid terlebih dahulu"
        
        binding.cardFertilityStatus.visibility = View.GONE
    }

    private fun checkUserMode() {
        lifecycleScope.launch {
            val prefs = getSharedPreferences("cycle_sync_prefs", MODE_PRIVATE)
            val activeEmail = prefs.getString("active_user_email", "aisyah@email.com") ?: "aisyah@email.com"
            val user = userRepository.getUserByEmail(activeEmail) ?: userRepository.getUser()
            user?.let {
                binding.tvUserName.text = it.name ?: "Aisyah"
                updateDashboardMode(it.isPregnant)
            }
        }
    }

    private fun updateDashboardMode(isPregnant: Boolean) {
        isUserPregnant = isPregnant
        if (isPregnant) {
            binding.dashHero.setBackgroundResource(R.drawable.bg_gradient_purple)
            binding.tvGreeting.text = "Usia Kehamilan Anda,"
            binding.tvPhaseBadge.text = "Mode Kehamilan"
            binding.tvNavSiklus.text = "Fase"
            
            // Sembunyikan card kesuburan yang tidak relevan saat hamil
            binding.cardFertilityStatus.visibility = View.GONE
            
            // Ubah teks menu Smart Food menjadi Nutrisi
            binding.tvNavFoodLabel.text = "Nutrisi"
            
            // Ubah deskripsi di akses cepat (Smart Food & Log) untuk kehamilan
            val smartFoodTitle = binding.cardSmartFood.getChildAt(1) as? TextView
            val smartFoodDesc = binding.cardSmartFood.getChildAt(2) as? TextView
            smartFoodTitle?.text = "Nutrisi Hamil"
            smartFoodDesc?.text = "Gizi & vitamin ibu hamil"
            
            val logTitle = binding.cardLogHarian.getChildAt(1) as? TextView
            val logDesc = binding.cardLogHarian.getChildAt(2) as? TextView
            logTitle?.text = "Log Gejala"
            logDesc?.text = "Catat gejala kehamilan"

            // Update Toggle Card State
            binding.ivModeIcon.setImageResource(R.drawable.ic_heart)
            binding.ivModeIcon.imageTintList = ContextCompat.getColorStateList(this, R.color.purple)
            binding.tvModeTitle.text = "Status Kehamilan"
            binding.tvModeDesc.text = "Mode Kehamilan Aktif"
            binding.btnToggleMode.text = "Akhiri"
            binding.btnToggleMode.backgroundTintList = ContextCompat.getColorStateList(this, R.color.purple)

            // Make prediction card clickable to open PregnancyHplActivity
            binding.cardPrediction.setOnClickListener {
                startActivity(Intent(this, PregnancyHplActivity::class.java))
            }

            // Update active bottom nav colors to purple
            val homeIcon = binding.navBeranda.getChildAt(0) as? ImageView
            homeIcon?.imageTintList = ContextCompat.getColorStateList(this, R.color.purple)
            val homeLabel = binding.navBeranda.getChildAt(1) as? TextView
            homeLabel?.setTextColor(ContextCompat.getColor(this, R.color.purple))

            // Hitung usia kehamilan
            lifecycleScope.launch {
                val database = CycleDatabase.getDatabase(this@DashboardActivity)
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
                        
                        binding.tvCycleDayValue.text = "${weeks}w ${days}d"
                        binding.tvCycleDayLabel.text = "Minggu Kehamilan"
                        
                        val progress = ((weeks.toFloat() / 40f) * 100).toInt().coerceIn(0, 100)
                        binding.cycleRing.progress = progress
                        
                        binding.tvStat1Label.text = "Hari Hamil"
                        binding.tvStat1Value.text = "$totalDays hr"
                        
                        val trimester = when {
                            weeks <= 12 -> "1"
                            weeks <= 27 -> "2"
                            else -> "3"
                        }
                        binding.tvStat2Label.text = "Trimester"
                        binding.tvStat2Value.text = trimester
                        
                        val hplCal = lmp.clone() as Calendar
                        hplCal.add(Calendar.DAY_OF_YEAR, 280)
                        val dayOfMonth = hplCal.get(Calendar.DAY_OF_MONTH)
                        val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
                        val hplStr = "$dayOfMonth ${monthNames[hplCal.get(Calendar.MONTH)]}"
                        
                        binding.tvStat3Label.text = "HPL"
                        binding.tvStat3Value.text = hplStr

                        binding.tvNextPeriodPrediction.text = "Hari Perkiraan Lahir (HPL): $hplStr"
                        binding.tvNextPeriodPrediction.setTextColor(ContextCompat.getColor(this@DashboardActivity, R.color.purple))
                        binding.tvFertileWindowPrediction.text = "Tetap sehat & bugar selama masa kehamilan"
                    }
                }
            }
        } else {
            binding.dashHero.setBackgroundResource(R.drawable.bg_gradient_pink)
            binding.tvGreeting.text = "Selamat pagi,"
            binding.tvNavSiklus.text = "Siklus"
            binding.cardFertilityStatus.visibility = View.VISIBLE
            binding.tvNavFoodLabel.text = "Food"
            
            val smartFoodTitle = binding.cardSmartFood.getChildAt(1) as? TextView
            val smartFoodDesc = binding.cardSmartFood.getChildAt(2) as? TextView
            smartFoodTitle?.text = "Smart Food"
            smartFoodDesc?.text = "Rekomendasi fase siklus"
            
            val logTitle = binding.cardLogHarian.getChildAt(1) as? TextView
            val logDesc = binding.cardLogHarian.getChildAt(2) as? TextView
            logTitle?.text = "Log Harian"
            logDesc?.text = "Catat mood & gejala hari ini"

            binding.tvStat1Label.text = "Panjang Siklus"
            binding.tvStat2Label.text = "Hari Tersisa"
            binding.tvStat3Label.text = "Berikutnya"
            binding.tvNextPeriodPrediction.setTextColor(ContextCompat.getColor(this, R.color.p))

            // Update Toggle Card State
            binding.ivModeIcon.setImageResource(R.drawable.ic_heart)
            binding.ivModeIcon.imageTintList = ContextCompat.getColorStateList(this, R.color.p)
            binding.tvModeTitle.text = "Status Kehamilan"
            binding.tvModeDesc.text = "Aktifkan Mode Kehamilan"
            binding.btnToggleMode.text = "Aktifkan"
            binding.btnToggleMode.backgroundTintList = ContextCompat.getColorStateList(this, R.color.p)

            // Sembunyikan click prediction card saat mode haid biasa
            binding.cardPrediction.setOnClickListener(null)

            // Restore active bottom nav colors to pink
            val homeIcon = binding.navBeranda.getChildAt(0) as? ImageView
            homeIcon?.imageTintList = ContextCompat.getColorStateList(this, R.color.p)
            val homeLabel = binding.navBeranda.getChildAt(1) as? TextView
            homeLabel?.setTextColor(ContextCompat.getColor(this, R.color.p))
        }
    }

    private fun setupUI() {
        binding.cardLogHarian.setOnClickListener {
            startActivity(Intent(this, DailyLogActivity::class.java))
        }
        binding.navLog.setOnClickListener {
            startActivity(Intent(this, DailyLogActivity::class.java))
        }
        binding.navSiklus.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }
        binding.cardSmartFood.setOnClickListener {
            startActivity(Intent(this, SmartFoodActivity::class.java))
        }
        binding.navFood.setOnClickListener {
            startActivity(Intent(this, SmartFoodActivity::class.java))
        }
        binding.cardEdukasi.setOnClickListener {
            startActivity(Intent(this, EducationActivity::class.java))
        }
        binding.navEdukasi.setOnClickListener {
            startActivity(Intent(this, EducationActivity::class.java))
        }
        binding.cardPartnerSync.setOnClickListener {
            startActivity(Intent(this, PartnerSyncActivity::class.java))
        }
        
        binding.cardFertilityStatus.setOnClickListener {
             // Future: Go to Fertility Notif screen
        }

        binding.btnToggleMode.setOnClickListener {
            if (isUserPregnant) {
                // Akhiri mode kehamilan
                startActivity(Intent(this, PostpartumActivity::class.java))
            } else {
                // Aktifkan mode kehamilan
                showPregnancyActivationDialog()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun setupRecyclerView() {
        // Since we removed rvRecommendations for a custom layout, we can leave this empty or remove calls
    }

    private fun setupObservers() {
        // Observasi List Rekomendasi (Arin)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                recommendationViewModel.recommendations.collect { recommendations ->
                    // Update the static cards with the first items from the recommendation list
                    if (recommendations.isNotEmpty()) {
                        // Assuming the first is nutrition and the second is exercise for simplicity in this prototype
                        val nutrition = recommendations.find { it.category.contains("food", ignoreCase = true) || it.category.contains("nutrisi", ignoreCase = true) }
                        val exercise = recommendations.find { it.category.contains("exercise", ignoreCase = true) || it.category.contains("olahraga", ignoreCase = true) }

                        nutrition?.let {
                            binding.tvRecommendationNutrisiContent.text = it.description
                        }
                        exercise?.let {
                            binding.tvRecommendationExerciseContent.text = it.description
                        }
                    }
                }
            }
        }

        // Observasi Perubahan Siklus
        cycleViewModel.predictionResult.observe(this) { result ->
            // Update Rekomendasi berdasarkan fase otomatis
            val phaseName = if (result.currentPhase == com.example.cyclesyncapp.domain.model.HormonalPhase.PREGNANCY) {
                val weeks = result.dayOfCycle / 7
                val trimester = when {
                    weeks <= 12 -> 1
                    weeks <= 27 -> 2
                    else -> 3
                }
                "PREGNANCY_T$trimester"
            } else {
                result.currentPhase.name
            }
            recommendationViewModel.loadRecommendations(phaseName)

            if (result.entryCount == 0) {
                showNoPredictionsOnDashboard()
                binding.tvPredictionConfidence.text = result.confidenceLevel
                binding.tvCycleRegularity.text = "Keteraturan: ${result.regularityStatus}"
                binding.tvPredictionDisclaimer.text = result.disclaimerText
                binding.pbAccuracyProgress.progress = result.progressPercent
                binding.tvAccuracyProgressText.text = result.progressText
                return@observe
            }

            val today = Calendar.getInstance()
            val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

            if (result.currentPhase == com.example.cyclesyncapp.domain.model.HormonalPhase.PREGNANCY) {
                binding.tvGreeting.text = "Usia Kehamilan Anda,"
                binding.tvPhaseBadge.text = "Mode Kehamilan"
                binding.tvCycleDayLabel.text = "Hari Kehamilan"
                binding.cardWelcomeNotif.visibility = View.GONE
                
                binding.tvCycleDayValue.text = "${result.dayOfCycle / 7}w ${result.dayOfCycle % 7}d"
                val progress = ((result.dayOfCycle.toFloat() / 280f) * 100).toInt().coerceIn(0, 100)
                binding.cycleRing.progress = progress
                
                binding.tvStat1Label.text = "Hari Hamil"
                binding.tvStat1Value.text = "${result.dayOfCycle} hr"
                
                val weeks = result.dayOfCycle / 7
                val trimester = when {
                    weeks <= 12 -> "1"
                    weeks <= 27 -> "2"
                    else -> "3"
                }
                binding.tvStat2Label.text = "Trimester"
                binding.tvStat2Value.text = trimester
                
                val hplCal = Calendar.getInstance().apply { timeInMillis = result.nextPeriodDate }
                val hplStr = "${hplCal.get(Calendar.DAY_OF_MONTH)} ${monthNames[hplCal.get(Calendar.MONTH)]}"
                binding.tvStat3Label.text = "HPL"
                binding.tvStat3Value.text = hplStr

                binding.tvNextPeriodPrediction.text = "Hari Perkiraan Lahir (HPL): $hplStr"
                binding.tvFertileWindowPrediction.text = "Tetap sehat & bugar selama masa kehamilan"
                
                binding.tvPredictionConfidence.text = "Hamil"
                binding.tvCycleRegularity.text = "Keteraturan: N/A"
                binding.tvPredictionDisclaimer.text = result.disclaimerText
                binding.pbAccuracyProgress.progress = 100
                binding.tvAccuracyProgressText.text = "Pelacakan Kehamilan Aktif"
            } else {
                binding.tvGreeting.text = "Selamat pagi,"
                binding.cardFertilityStatus.visibility = View.VISIBLE
                
                val phaseNameFormatted = result.currentPhase.name.lowercase().replaceFirstChar { it.uppercase() }
                binding.tvPhaseBadge.text = "Fase $phaseNameFormatted"
                binding.tvCycleDayValue.text = "${result.dayOfCycle}"
                binding.tvCycleDayLabel.text = "Hari ke-${result.dayOfCycle}"

                // Circular Progress Ring
                val progress = (result.dayOfCycle.toFloat() / result.averageCycle.toFloat() * 100).toInt()
                binding.cycleRing.progress = progress.coerceIn(0, 100)

                // Stats Update
                binding.tvStat1Label.text = "Rerata Siklus"
                binding.tvStat1Value.text = "${result.averageCycle} hr"
                
                if (result.hasPrediction) {
                    val diff = result.nextPeriodDate - today.timeInMillis
                    val daysRemaining = (diff / (24 * 60 * 60 * 1000)).toInt()
                    binding.tvStat2Label.text = "Hari Tersisa"
                    binding.tvStat2Value.text = if (daysRemaining > 0) "$daysRemaining" else "0"

                    // Format Next Period Date Range
                    val rangeStart = Calendar.getInstance().apply { timeInMillis = result.predictionRangeStart }
                    val rangeEnd = Calendar.getInstance().apply { timeInMillis = result.predictionRangeEnd }
                    val startDay = rangeStart.get(Calendar.DAY_OF_MONTH)
                    val startMonth = monthNames[rangeStart.get(Calendar.MONTH)]
                    val endDay = rangeEnd.get(Calendar.DAY_OF_MONTH)
                    val endMonth = monthNames[rangeEnd.get(Calendar.MONTH)]
                    
                    val rangeStr = if (startMonth == endMonth) {
                        "$startDay - $endDay $startMonth"
                    } else {
                        "$startDay $startMonth - $endDay $endMonth"
                    }

                    val nextPeriod = Calendar.getInstance().apply { timeInMillis = result.nextPeriodDate }
                    val nextPeriodStr = "${nextPeriod.get(Calendar.DAY_OF_MONTH)} ${monthNames[nextPeriod.get(Calendar.MONTH)]}"
                    binding.tvStat3Label.text = "Berikutnya"
                    binding.tvStat3Value.text = nextPeriodStr

                    binding.tvNextPeriodPrediction.text = "Haid berikutnya diprediksi: $rangeStr"

                    // Format Fertile Window
                    val fertileStart = Calendar.getInstance().apply { timeInMillis = result.fertileStart }
                    val fertileEnd = Calendar.getInstance().apply { timeInMillis = result.fertileEnd }
                    val fertileStr = "${fertileStart.get(Calendar.DAY_OF_MONTH)} ${monthNames[fertileStart.get(Calendar.MONTH)]} - ${fertileEnd.get(Calendar.DAY_OF_MONTH)} ${monthNames[fertileEnd.get(Calendar.MONTH)]}"
                    binding.tvFertileWindowPrediction.text = "Masa subur berikutnya: $fertileStr"

                    // Check if Period is Late
                    val daysLate = (-daysRemaining)
                    if (daysLate >= 3) {
                        binding.cardWelcomeNotif.visibility = View.VISIBLE
                        binding.tvWelcomeEmoji.visibility = View.VISIBLE
                        binding.tvWelcomeEmoji.text = "⚠️"
                        binding.tvWelcomeTitle.text = "Haid Terlambat"
                        binding.tvWelcomeSubtitle.text = "Siklus haid Anda terlambat $daysLate hari. Apakah Anda ingin melakukan tes kehamilan?"
                        binding.cardWelcomeNotif.setOnClickListener {
                            startActivity(Intent(this, PregnancyDetectActivity::class.java))
                        }
                    } else {
                        binding.cardWelcomeNotif.visibility = View.VISIBLE
                        binding.tvWelcomeEmoji.visibility = View.GONE
                        binding.tvWelcomeTitle.text = "Profil Berhasil Dibuat"
                        binding.tvWelcomeSubtitle.text = "Selamat datang di CycleSync! Mulai perjalanan kesehatan hormonal Anda."
                        binding.cardWelcomeNotif.setOnClickListener(null)
                    }
                } else {
                    binding.tvStat2Label.text = "Hari Tersisa"
                    binding.tvStat2Value.text = "--"
                    binding.tvStat3Label.text = "Berikutnya"
                    binding.tvStat3Value.text = "--"

                    binding.tvNextPeriodPrediction.text = "Prediksi haid: silakan catat minimal 2 haid"
                    binding.tvFertileWindowPrediction.text = "Prediksi masa subur: silakan catat minimal 2 haid"
                    binding.cardWelcomeNotif.visibility = View.GONE
                }

                // Update Accuracy UI components
                binding.tvPredictionConfidence.text = result.confidenceLevel
                binding.tvCycleRegularity.text = "Keteraturan: ${result.regularityStatus}"
                binding.tvPredictionDisclaimer.text = result.disclaimerText
                binding.pbAccuracyProgress.progress = result.progressPercent
                binding.tvAccuracyProgressText.text = result.progressText
            }

            // Jadwalkan Notifikasi
            if (result.hasPrediction && result.fertileStart > 0L) {
                val fertileStartCal = Calendar.getInstance().apply { timeInMillis = result.fertileStart }
                val fertileEndCal = Calendar.getInstance().apply { timeInMillis = result.fertileEnd }
                notificationScheduler.scheduleFertileWindowNotification(fertileStartCal, fertileEndCal)
            }
        }
    }

    private fun setupTutorial() {
        val prefs = getSharedPreferences("cycle_sync_prefs", MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("is_first_launch", true)
        if (!isFirstLaunch) return

        val overlay = findViewById<View>(R.id.layoutTutorialOverlay)
        val tvTitle = findViewById<TextView>(R.id.tvTutorialTitle)
        val tvDesc = findViewById<TextView>(R.id.tvTutorialDesc)
        val tvStep = findViewById<TextView>(R.id.tvTutorialStep)
        val btnSkip = findViewById<View>(R.id.btnTutorialSkip)
        val btnNext = findViewById<Button>(R.id.btnTutorialNext)

        overlay.visibility = View.VISIBLE

        var step = 1

        val steps = listOf(
            Triple("Selamat Datang di CycleSync", "Aplikasi asisten kesehatan hormonal Anda. Mari pelajari fitur utama kami.", "Langkah 1 dari 4"),
            Triple("Pemantauan Siklus (Cycle Ring)", "Di bagian atas, Ring Siklus menampilkan hari siklus Anda saat ini dan fase hormonal aktif Anda.", "Langkah 2 dari 4"),
            Triple("Log Harian & Perasaan", "Catat kondisi emosional, mood, tingkat energi, serta cerita harian Anda di menu 'Log Harian' untuk melatih kesadaran diri.", "Langkah 3 dari 4"),
            Triple("Rekomendasi Pintar & Edukasi", "Buka 'Smart Food' untuk rekomendasi gizi fase aktif Anda, dan temukan artikel kesehatan tepercaya di 'Edukasi'.", "Langkah 4 dari 4")
        )

        fun updateStep() {
            val current = steps[step - 1]
            tvTitle.text = current.first
            tvDesc.text = current.second
            tvStep.text = current.third
            btnNext.text = if (step == 4) "Mulai" else "Lanjut"
        }

        btnNext.setOnClickListener {
            if (step < 4) {
                step++
                updateStep()
            } else {
                overlay.visibility = View.GONE
                prefs.edit().putBoolean("is_first_launch", false)
                    .putInt("onboarding_step", 1)
                    .apply()
                checkOnboardingTasks()
            }
        }

        btnSkip.setOnClickListener {
            overlay.visibility = View.GONE
            prefs.edit().putBoolean("is_first_launch", false)
                .putInt("onboarding_step", -1)
                .apply()
            checkOnboardingTasks()
        }

        updateStep()
    }

    override fun onResume() {
        super.onResume()
        checkOnboardingTasks()
        checkUserMode()
        loadUserCycleData(database)
    }

    private fun checkOnboardingTasks() {
        val prefs = getSharedPreferences("cycle_sync_prefs", MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("is_first_launch", true)
        
        // Hide old checklist card completely
        binding.cardOnboardingChecklist.visibility = View.GONE

        if (isFirstLaunch) {
            binding.cardDemoGuide.visibility = View.GONE
            return
        }

        // Get current onboarding step
        val onboardingStep = prefs.getInt("onboarding_step", 1)
        val pregnancyStep = prefs.getInt("pregnancy_onboarding_step", -1)

        if (pregnancyStep == 1) {
            binding.cardDemoGuide.visibility = View.VISIBLE
            binding.tvDemoGuideTitle.text = "Demo Kehamilan: Langkah 1 dari 4"
            binding.tvDemoGuideDesc.text = "Klik menu 'Siklus' di bawah untuk melihat Kalender Kehamilan Anda."
            
            // Highlight navSiklus
            binding.navSiklus.setBackgroundResource(R.drawable.bg_card_selected)
            binding.cardLogHarian.setBackgroundResource(R.drawable.bg_quick_card)
            binding.layoutTutorialCompleteOverlay.visibility = View.GONE
        } else if (pregnancyStep == 2) {
            binding.cardDemoGuide.visibility = View.VISIBLE
            binding.tvDemoGuideTitle.text = "Demo Kehamilan: Langkah 3 dari 4"
            binding.tvDemoGuideDesc.text = "Klik tombol 'Log Gejala' di Dashboard untuk mencatat kondisi psikologis Anda."
            
            // Highlight cardLogHarian and restore navSiklus background
            binding.navSiklus.background = null
            binding.cardLogHarian.setBackgroundResource(R.drawable.bg_card_selected)
            binding.layoutTutorialCompleteOverlay.visibility = View.GONE
        } else if (pregnancyStep == 3) {
            binding.cardDemoGuide.visibility = View.GONE
            binding.navSiklus.background = null
            binding.cardLogHarian.setBackgroundResource(R.drawable.bg_quick_card)
            
            // Show congrats popup for pregnancy
            if (binding.layoutTutorialCompleteOverlay.visibility != View.VISIBLE) {
                binding.layoutTutorialCompleteOverlay.visibility = View.VISIBLE
                playCelebrationSound()
            }
            
            val cardView = binding.layoutTutorialCompleteOverlay.getChildAt(0) as? androidx.cardview.widget.CardView
            val linearLayout = cardView?.getChildAt(0) as? android.widget.LinearLayout
            val tvCongratsEmoji = linearLayout?.getChildAt(0) as? TextView
            val tvCongratsTitle = linearLayout?.getChildAt(1) as? TextView
            val tvCongratsDesc = linearLayout?.getChildAt(2) as? TextView
            val btnCongratsClose = binding.btnTutorialCompleteClose
            
            tvCongratsEmoji?.setTextColor(ContextCompat.getColor(this, R.color.purple))
            tvCongratsTitle?.text = "Panduan Kehamilan Selesai!"
            tvCongratsDesc?.text = "Selamat! Anda telah mempelajari seluruh fitur dasar Mode Kehamilan di CycleSync dan siap untuk memantau perkembangan buah hati Anda secara mandiri."
            btnCongratsClose.backgroundTintList = ContextCompat.getColorStateList(this, R.color.purple)
            btnCongratsClose.text = "Mulai Eksplorasi Kehamilan"
            
            btnCongratsClose.setOnClickListener {
                binding.layoutTutorialCompleteOverlay.visibility = View.GONE
                prefs.edit().putInt("pregnancy_onboarding_step", -1).apply()
                
                lifecycleScope.launch {
                    try {
                        loadUserCycleData(database)
                        checkOnboardingTasks()
                        Toast.makeText(this@DashboardActivity, "Tutorial selesai! Memulai pemantauan kehamilan mandiri.", Toast.LENGTH_SHORT).show()
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } else if (onboardingStep == 1) {
            binding.cardDemoGuide.visibility = View.VISIBLE
            binding.tvDemoGuideTitle.text = "Langkah 1 dari 4: Buka Menu Siklus"
            binding.tvDemoGuideDesc.text = "Klik menu 'Siklus' di bawah untuk mulai demo."
            
            // Highlight navSiklus
            binding.navSiklus.setBackgroundResource(R.drawable.bg_card_selected)
            binding.cardLogHarian.setBackgroundResource(R.drawable.bg_quick_card)
            binding.layoutTutorialCompleteOverlay.visibility = View.GONE
        } else if (onboardingStep == 2) {
            binding.cardDemoGuide.visibility = View.VISIBLE
            binding.tvDemoGuideTitle.text = "Langkah 3 dari 4: Catat Log Harian"
            binding.tvDemoGuideDesc.text = "Klik tombol 'Log Harian' di Dashboard untuk mencatat mood dan tingkat energi."
            
            // Highlight cardLogHarian and restore navSiklus background
            binding.navSiklus.background = null
            binding.cardLogHarian.setBackgroundResource(R.drawable.bg_card_selected)
            binding.layoutTutorialCompleteOverlay.visibility = View.GONE
        } else if (onboardingStep == 3) {
            binding.cardDemoGuide.visibility = View.GONE
            binding.navSiklus.background = null
            binding.cardLogHarian.setBackgroundResource(R.drawable.bg_quick_card)
            
            // Show congrats popup
            if (binding.layoutTutorialCompleteOverlay.visibility != View.VISIBLE) {
                binding.layoutTutorialCompleteOverlay.visibility = View.VISIBLE
                playCelebrationSound()
            }

            val cardView = binding.layoutTutorialCompleteOverlay.getChildAt(0) as? androidx.cardview.widget.CardView
            val linearLayout = cardView?.getChildAt(0) as? android.widget.LinearLayout
            val tvCongratsEmoji = linearLayout?.getChildAt(0) as? TextView
            val tvCongratsTitle = linearLayout?.getChildAt(1) as? TextView
            val tvCongratsDesc = linearLayout?.getChildAt(2) as? TextView
            val btnCongratsClose = binding.btnTutorialCompleteClose

            tvCongratsEmoji?.setTextColor(ContextCompat.getColor(this, R.color.p))
            tvCongratsTitle?.text = "Panduan Selesai!"
            tvCongratsDesc?.text = "Selamat! Anda telah mempelajari seluruh fitur dasar CycleSync dan siap untuk memantau kesehatan hormonal Anda secara mandiri."
            btnCongratsClose.backgroundTintList = ContextCompat.getColorStateList(this, R.color.p)
            btnCongratsClose.text = "Mulai Eksplorasi Mandiri"

            btnCongratsClose.setOnClickListener {
                binding.layoutTutorialCompleteOverlay.visibility = View.GONE
                prefs.edit().putInt("onboarding_step", -1).apply()
                
                // Clear the database tables to go back to initial empty state!
                lifecycleScope.launch {
                    try {
                        database.dailyLogDao().clearLogs()
                        database.cycleDao().clearCycles()
                        loadUserCycleData(database)
                        checkOnboardingTasks()
                        Toast.makeText(this@DashboardActivity, "Tutorial selesai! Memulai eksplorasi mandiri.", Toast.LENGTH_SHORT).show()
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } else {
            // Tutorial finished or not active
            binding.cardDemoGuide.visibility = View.GONE
            binding.navSiklus.background = null
            binding.cardLogHarian.setBackgroundResource(R.drawable.bg_quick_card)
            binding.layoutTutorialCompleteOverlay.visibility = View.GONE
            binding.btnToggleMode.setOnClickListener {
                if (isUserPregnant) {
                    // Akhiri mode kehamilan
                    startActivity(Intent(this, PostpartumActivity::class.java))
                } else {
                    // Aktifkan mode kehamilan
                    showPregnancyActivationDialog()
                }
            }
        }
    }

    private fun showPregnancyActivationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_pregnancy_activation, null)
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        val rgInputType = dialogView.findViewById<android.widget.RadioGroup>(R.id.rgInputType)
        val rbLmpDate = dialogView.findViewById<android.widget.RadioButton>(R.id.rbLmpDate)
        val rbGestationalAge = dialogView.findViewById<android.widget.RadioButton>(R.id.rbGestationalAge)
        val layoutLmpDateInput = dialogView.findViewById<android.widget.LinearLayout>(R.id.layoutLmpDateInput)
        val layoutGestationalAgeInput = dialogView.findViewById<android.widget.LinearLayout>(R.id.layoutGestationalAgeInput)
        val tvSelectedLmpDate = dialogView.findViewById<android.widget.TextView>(R.id.tvSelectedLmpDate)
        val etWeeks = dialogView.findViewById<android.widget.EditText>(R.id.etWeeks)
        val etDays = dialogView.findViewById<android.widget.EditText>(R.id.etDays)
        val btnCancel = dialogView.findViewById<android.widget.Button>(R.id.btnCancel)
        val btnConfirm = dialogView.findViewById<android.widget.Button>(R.id.btnConfirm)

        var selectedLmpCal = Calendar.getInstance()
        val displayFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
        tvSelectedLmpDate.text = displayFormat.format(selectedLmpCal.time)

        rgInputType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbLmpDate) {
                layoutLmpDateInput.visibility = View.VISIBLE
                layoutGestationalAgeInput.visibility = View.GONE
            } else {
                layoutLmpDateInput.visibility = View.GONE
                layoutGestationalAgeInput.visibility = View.VISIBLE
            }
        }

        tvSelectedLmpDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(this, { _, year, month, day ->
                selectedLmpCal.set(year, month, day)
                tvSelectedLmpDate.text = displayFormat.format(selectedLmpCal.time)
            }, selectedLmpCal.get(Calendar.YEAR), selectedLmpCal.get(Calendar.MONTH), selectedLmpCal.get(Calendar.DAY_OF_MONTH))
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            datePickerDialog.show()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            var lmpString = ""
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

            if (rbLmpDate.isChecked) {
                lmpString = sdf.format(selectedLmpCal.time)
            } else {
                val weeksStr = etWeeks.text.toString().trim()
                val daysStr = etDays.text.toString().trim()
                val weeks = weeksStr.toIntOrNull() ?: 0
                val days = daysStr.toIntOrNull() ?: 0

                if (weeks == 0 && days == 0) {
                    Toast.makeText(this, "Silakan isi usia kehamilan Anda", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (weeks < 0 || weeks > 42) {
                    Toast.makeText(this, "Usia kehamilan (minggu) tidak valid (0 - 42)", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (days < 0 || days > 6) {
                    Toast.makeText(this, "Hari tidak valid (0 - 6)", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val calculatedLmp = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -(weeks * 7 + days))
                }
                lmpString = sdf.format(calculatedLmp.time)
            }

            lifecycleScope.launch {
                try {
                    val prefs = getSharedPreferences("cycle_sync_prefs", MODE_PRIVATE)
                    prefs.edit().putString("pregnancy_onboarding_lmp", lmpString)
                        .putInt("pregnancy_onboarding_step", 1)
                        .apply()

                    val activeEmail = prefs.getString("active_user_email", "aisyah@email.com") ?: "aisyah@email.com"
                    val user = userRepository.getUserByEmail(activeEmail) ?: userRepository.getUser()
                    if (user != null) {
                        userRepository.updateUser(user.copy(isPregnant = true))
                    }

                    database.cycleDao().insertCycle(
                        CycleEntity(
                            startDate = lmpString,
                            endDate = lmpString,
                            cycleLength = 28,
                            periodLength = 5,
                            notes = "PREGNANCY_LMP"
                        )
                    )

                    Toast.makeText(this@DashboardActivity, "Mode Kehamilan Berhasil Diaktifkan!", Toast.LENGTH_SHORT).show()
                    triggerPregnancyTransitionOverlay()
                    updateDashboardMode(true)
                    loadUserCycleData(database)
                    dialog.dismiss()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@DashboardActivity, "Gagal mengaktifkan mode kehamilan", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun triggerPregnancyTransitionOverlay() {
        binding.composeTransitionOverlay.visibility = View.VISIBLE
        binding.composeTransitionOverlay.setContent {
            var showTransition by remember { mutableStateOf(true) }
            val rippleRadius = remember { Animatable(0f) }
            val particles = remember { mutableStateListOf<BurstParticle>() }

            LaunchedEffect(Unit) {
                playDynamicUnlockSound()
                launch {
                    rippleRadius.animateTo(
                        targetValue = 2800f,
                        animationSpec = tween(durationMillis = 3000, easing = LinearOutSlowInEasing)
                    )
                }

                particles.clear()
                val random = java.util.Random()
                for (i in 0..85) {
                    val angle = random.nextFloat() * 2 * PI
                    val speed = 5f + random.nextFloat() * 15f
                    val size = 5f + random.nextFloat() * 12f
                    val color = when (random.nextInt(3)) {
                        0 -> Color(0xFFD0BCFF)
                        1 -> Color(0xFFFFD700)
                        else -> Color.White
                    }
                    particles.add(
                        BurstParticle(
                            x = 0f, y = 0f,
                            vx = (cos(angle) * speed).toFloat(),
                            vy = (sin(angle) * speed).toFloat(),
                            color = color,
                            size = size
                        )
                    )
                }

                val duration = 3000L
                val start = System.currentTimeMillis()
                while (System.currentTimeMillis() - start < duration) {
                    delay(16)
                    for (p in particles) {
                        p.x += p.vx
                        p.y += p.vy
                        p.alpha = (p.alpha - 0.007f).coerceAtLeast(0f)
                    }
                }
                
                binding.composeTransitionOverlay.visibility = View.GONE
                checkOnboardingTasks()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1B0B30)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f

                    if (rippleRadius.value > 0f) {
                        drawCircle(
                            color = Color(0xFFD0BCFF).copy(
                                alpha = (1f - (rippleRadius.value / 2800f)).coerceAtLeast(0f)
                            ),
                            radius = rippleRadius.value,
                            center = Offset(cx, cy),
                            style = Stroke(width = 18f)
                        )
                        drawCircle(
                            color = Color(0xFFFFD700).copy(
                                alpha = (1f - (rippleRadius.value / 2800f)).coerceAtLeast(0f)
                            ),
                            radius = (rippleRadius.value - 200f).coerceAtLeast(0f),
                            center = Offset(cx, cy),
                            style = Stroke(width = 10f)
                        )
                    }

                    particles.forEach { p ->
                        if (p.alpha > 0f) {
                            drawCircle(
                                color = p.color.copy(alpha = p.alpha),
                                radius = p.size,
                                center = Offset(cx + p.x, cy + p.y)
                            )
                        }
                    }
                }

                val badgeScale = animateFloatAsState(
                    targetValue = if (rippleRadius.value > 1200f) 0f else 1f,
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.scale(badgeScale.value)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFF7B5EA7), Color(0xFF4A148C))
                                )
                            )
                            .padding(12.dp)
                    ) {
                        Canvas(modifier = Modifier.size(60.dp)) {
                            val w = size.width
                            val h = size.height
                            val path = Path().apply {
                                addRoundRect(
                                    androidx.compose.ui.geometry.RoundRect(
                                        left = w * 0.15f,
                                        top = h * 0.45f,
                                        right = w * 0.85f,
                                        bottom = h * 0.95f,
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                                    )
                                )
                                moveTo(w * 0.3f, h * 0.45f)
                                lineTo(w * 0.3f, h * 0.25f)
                                cubicTo(w * 0.3f, h * 0.1f, w * 0.7f, h * 0.1f, w * 0.7f, h * 0.25f)
                                lineTo(w * 0.7f, h * 0.35f)
                            }
                            drawPath(path, color = Color(0xFFFFD700), style = Stroke(width = 8f))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    androidx.compose.material3.Text(
                        text = "MEMBUKA MODE BARU",
                        color = Color(0xFFFFD700),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        letterSpacing = 3.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    androidx.compose.material3.Text(
                        text = "MODE KEHAMILAN AKTIF",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }

    private fun playDynamicUnlockSound() {
        lifecycleScope.launch(Dispatchers.IO) {
            val sampleRate = 44100
            val durationMs = 3000
            val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
            val buffer = ShortArray(numSamples)
            
            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                var value = 0.0
                var envelope = 1.0

                if (t < 0.25f) {
                    // Bloop 1
                    val freq = 380f + (t / 0.25f) * 380f
                    value = sin(2.0 * PI * freq * t)
                    envelope = exp(-9.0 * (t % 0.25f).toDouble())
                } else if (t < 0.5f) {
                    // Bloop 2
                    val tLocal = t - 0.25f
                    val freq = 480f + (tLocal / 0.25f) * 480f
                    value = sin(2.0 * PI * freq * tLocal)
                    envelope = exp(-9.0 * tLocal.toDouble())
                } else if (t < 0.8f) {
                    // Bloop 3
                    val tLocal = t - 0.5f
                    val freq = 600f + (tLocal / 0.3f) * 600f
                    value = sin(2.0 * PI * freq * tLocal)
                    envelope = exp(-9.0 * tLocal.toDouble())
                } else {
                    // Sparkling magical chime C major chord
                    val tLocal = t - 0.8f
                    val freqs = listOf(1046.50f, 1318.51f, 1567.98f, 2093.00f, 2637.02f)
                    var chord = 0.0
                    for (f in freqs) {
                        chord += sin(2.0 * PI * f * tLocal)
                    }
                    value = chord / freqs.size
                    envelope = exp(-1.8 * tLocal.toDouble())
                }
                
                val pcm = (value * envelope * 32767.0 * 0.65f).toInt().coerceIn(-32768, 32767)
                buffer[i] = pcm.toShort()
            }
            
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
                delay(durationMs.toLong() + 100)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playCelebrationSound() {
        lifecycleScope.launch(Dispatchers.IO) {
            val sampleRate = 44100
            val durationMs = 2000
            val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
            val buffer = ShortArray(numSamples)
            
            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                var value = 0.0
                var envelope = 1.0
                
                if (t < 0.6f) {
                    // Cartoon Spring "Boing-oing-oing"
                    val baseFreq = 261.63f + (t / 0.6f) * 261.63f // slides C4 to C5
                    val wobble = 70f * sin(2.0 * PI * 22.0 * t) // 22Hz bounce wobble
                    val freq = baseFreq + wobble
                    value = sin(2.0 * PI * freq * t)
                    envelope = exp(-1.8 * t.toDouble())
                } else if (t < 1.0f) {
                    // Double Bubble Pop "pop-pop"
                    val tLocal = t - 0.6f
                    if (tLocal < 0.12f) {
                        // Pop 1
                        val freq = 880f
                        value = sin(2.0 * PI * freq * tLocal)
                        envelope = exp(-120.0 * tLocal.toDouble())
                    } else if (tLocal in 0.15f..0.27f) {
                        // Pop 2 (higher pitched)
                        val tPop2 = tLocal - 0.15f
                        val freq = 1174.66f // D6
                        value = sin(2.0 * PI * freq * tPop2)
                        envelope = exp(-120.0 * tPop2.toDouble())
                    } else {
                        value = 0.0
                        envelope = 0.0
                    }
                } else {
                    // Sparkling Chime
                    val tLocal = t - 1.0f
                    val freqs = listOf(1046.50f, 1318.51f, 1567.98f, 2093.00f)
                    var chord = 0.0
                    for (f in freqs) {
                        chord += sin(2.0 * PI * f * tLocal)
                    }
                    value = chord / freqs.size
                    envelope = exp(-3.0 * tLocal.toDouble())
                }
                
                val pcm = (value * envelope * 32767.0 * 0.65f).toInt().coerceIn(-32768, 32767)
                buffer[i] = pcm.toShort()
            }
            
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
                delay(durationMs.toLong() + 100)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class BurstParticle(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    var alpha: Float = 1f
)