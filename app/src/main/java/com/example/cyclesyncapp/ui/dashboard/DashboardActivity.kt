package com.example.cyclesyncapp.ui.dashboard

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
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.cyclesyncapp.R
import com.example.cyclesyncapp.data.local.database.CycleDatabase
import com.example.cyclesyncapp.data.repository.UserRepositoryImpl
import com.example.cyclesyncapp.databinding.ActivityDashboardBinding
import com.example.cyclesyncapp.data.local.notification.NotificationScheduler
import com.example.cyclesyncapp.domain.usecase.PartnerSyncUseCase
import com.example.cyclesyncapp.ui.adapter.RecommendationAdapter
import com.example.cyclesyncapp.ui.calendar.CalendarActivity
import com.example.cyclesyncapp.ui.viewmodel.CycleViewModel
import com.example.cyclesyncapp.ui.viewmodel.RecommendationViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

    private val recommendationViewModel: RecommendationViewModel by viewModels()
    private val cycleViewModel: CycleViewModel by viewModels()
    private val recommendationAdapter = RecommendationAdapter()
    private val partnerSyncUseCase = PartnerSyncUseCase()
    private lateinit var notificationScheduler: NotificationScheduler
    private lateinit var userRepository: UserRepositoryImpl
    private lateinit var database: CycleDatabase

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission result if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        if (isPregnant) {
            binding.tvGreeting.text = "Usia Kehamilan Anda,"
            binding.tvPhaseBadge.text = "Mode Kehamilan"
            
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
                        binding.tvFertileWindowPrediction.text = "Tetap sehat & bugar selama masa kehamilan"
                    }
                }
            }
        } else {
            binding.tvGreeting.text = "Selamat pagi,"
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
            recommendationViewModel.loadRecommendations(result.currentPhase.name)

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
                .putInt("onboarding_step", 1)
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

        if (onboardingStep == 1) {
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
            binding.layoutTutorialCompleteOverlay.visibility = View.VISIBLE
            binding.btnTutorialCompleteClose.setOnClickListener {
                binding.layoutTutorialCompleteOverlay.visibility = View.GONE
                prefs.edit().putInt("onboarding_step", -1).apply()
            }
        } else {
            // Tutorial finished or not active
            binding.cardDemoGuide.visibility = View.GONE
            binding.navSiklus.background = null
            binding.cardLogHarian.setBackgroundResource(R.drawable.bg_quick_card)
            binding.layoutTutorialCompleteOverlay.visibility = View.GONE
        }
    }
}