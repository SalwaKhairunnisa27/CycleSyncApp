package com.example.cyclesyncapp.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cyclesyncapp.data.local.database.CycleDatabase
import com.example.cyclesyncapp.data.security.EncryptionManager
import com.example.cyclesyncapp.databinding.ActivityDashboardBinding
import com.example.cyclesyncapp.ui.adapter.RecommendationAdapter
import com.example.cyclesyncapp.ui.calendar.CalendarActivity
import com.example.cyclesyncapp.ui.viewmodel.CycleViewModel
import com.example.cyclesyncapp.ui.viewmodel.RecommendationViewModel
import com.example.cyclesyncapp.worker.ReminderWorker
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

    private val recommendationViewModel: RecommendationViewModel by viewModels()
    private val cycleViewModel: CycleViewModel by viewModels()
    private val recommendationAdapter = RecommendationAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
        setupObservers()
        setupPartnerSync()
        scheduleReminders()

        // Calculate and load dynamic predictions
        val mainPrefs = getSharedPreferences("cyclesync_prefs", MODE_PRIVATE)
        val lastPeriodStr = mainPrefs.getString("last_period", "") ?: ""
        val cycleLength = mainPrefs.getInt("cycle_length", 28)

        if (lastPeriodStr.isNotEmpty()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val parsedDate = sdf.parse(lastPeriodStr)
                if (parsedDate != null) {
                    val cal = Calendar.getInstance()
                    cal.time = parsedDate
                    cycleViewModel.calculatePrediction(
                        year = cal.get(Calendar.YEAR),
                        month = cal.get(Calendar.MONTH),
                        day = cal.get(Calendar.DAY_OF_MONTH),
                        cycleHistory = listOf(cycleLength)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                recommendationViewModel.loadRecommendations("LUTEAL")
            }
        } else {
            recommendationViewModel.loadRecommendations("LUTEAL")
        }
    }

    private fun setupUI() {
        binding.cardLogHarian.setOnClickListener {
            startActivity(Intent(this, DailyLogActivity::class.java))
        }
        binding.navSiklus.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }
        binding.navLog.setOnClickListener {
            startActivity(Intent(this, DailyLogActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        binding.rvRecommendations.apply {
            adapter = recommendationAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupObservers() {
        // Observasi List Rekomendasi
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                recommendationViewModel.recommendations.collect { recommendations ->
                    recommendationAdapter.submitList(recommendations)
                }
            }
        }

        // Observasi Perubahan Siklus
        cycleViewModel.predictionResult.observe(this) { result ->
            // Update Rekomendasi berdasarkan fase otomatis
            recommendationViewModel.loadRecommendations(result.currentPhase.name)

            // Update Badge Fase di UI
            binding.tvPhaseBadge.text = "🌸 Fase ${result.currentPhase}"
            
            // Simpan fase saat ini ke SharedPreferences untuk digunakan di DailyLog
            val prefs = getSharedPreferences("cyclesync_prefs", MODE_PRIVATE)
            prefs.edit().putString("current_phase", result.currentPhase.name).apply()
        }
    }

    private fun setupPartnerSync() {
        binding.cardPartnerSync.setOnClickListener {
            startActivity(Intent(this, PartnerSyncActivity::class.java))
        }
    }

    private fun scheduleReminders() {
        try {
            android.util.Log.d("CYCLESYNC_NOTIF", "Mulai menjadwalkan ReminderWorker sebagai OneTime (Test)...")
            val reminderRequest = androidx.work.OneTimeWorkRequestBuilder<ReminderWorker>().build()

            WorkManager.getInstance(this).enqueueUniqueWork(
                "CycleSyncReminders",
                androidx.work.ExistingWorkPolicy.REPLACE,
                reminderRequest
            )
            android.util.Log.d("CYCLESYNC_NOTIF", "OneTime ReminderWorker berhasil didaftarkan! Akan berjalan langsung sekarang.")
        } catch (e: Exception) {
            android.util.Log.e("CYCLESYNC_NOTIF", "Gagal mendaftarkan ReminderWorker: ${e.message}")
            e.printStackTrace()
        }
    }
}