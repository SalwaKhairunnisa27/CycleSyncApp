package com.example.cyclesyncapp.ui.dashboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.example.cyclesyncapp.R
import com.example.cyclesyncapp.data.local.database.CycleDatabase
import com.example.cyclesyncapp.data.local.preferences.UserPreferences
import com.example.cyclesyncapp.domain.usecase.UserGoal
import com.example.cyclesyncapp.ui.calendar.CalendarActivity
import com.example.cyclesyncapp.ui.viewmodel.CycleViewModel
import com.example.cyclesyncapp.util.NotificationScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DashboardActivity : AppCompatActivity() {

    private lateinit var database: CycleDatabase
    private val viewModel: CycleViewModel by viewModels()

    // ✅ Launcher untuk minta permission notifikasi (Android 13+)
    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("NOTIF_PERM", "✅ Izin notifikasi diberikan user")
            scheduleNotifications()
        } else {
            Log.w("NOTIF_PERM", "⚠️ Izin notifikasi ditolak user — notifikasi tidak akan muncul")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val prefs = UserPreferences(this)
        val nickname = prefs.getNickname() ?: "Aisyah"

        // Greeting
        findViewById<TextView>(R.id.tvGreetingName)?.text = "$nickname ✨"

        // Inisialisasi database
        database = CycleDatabase.getDatabase(this)

        // ✅ Ambil data LMP nyata dari UserPreferences
        val lmpYear  = prefs.getLmpYear()
        val lmpMonth = prefs.getLmpMonth()
        val lmpDay   = prefs.getLmpDay()
        val cycleLen = prefs.getNextPeriodDays().takeIf { it > 0 } ?: 28

        // Tentukan goal
        val goalEnum = when (prefs.getGoal()) {
            "promil"  -> UserGoal.GET_PREGNANT
            "hindari" -> UserGoal.AVOID_PREGNANCY
            else      -> UserGoal.TRACK_CYCLE
        }

        // ✅ Ambil Settings Notifikasi
        val notifSettings = com.example.cyclesyncapp.domain.usecase.NotificationSettings(
            dailyLogEnabled = prefs.isDailyLogNotifEnabled(),
            periodRemindEnabled = prefs.isPeriodNotifEnabled(),
            supplementEnabled = prefs.isSupplementNotifEnabled()
        )

        // Kalkulasi prediksi siklus
        if (lmpYear != -1) {
            viewModel.calculatePrediction(lmpYear, lmpMonth, lmpDay, listOf(cycleLen), goalEnum, notifSettings)
        } else {
            // Fallback ke hari ini - 28 hari
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -28) }
            viewModel.calculatePrediction(
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH),
                listOf(28), goalEnum, notifSettings
            )
        }

        // Observe hasil prediksi → update UI & simpan fase ke prefs
        viewModel.predictionResult.observe(this) { result ->
            val now = System.currentTimeMillis()
            val daysToNextPeriod   = TimeUnit.MILLISECONDS.toDays(result.nextPeriodDate - now).toInt()
            val daysToFertileStart = TimeUnit.MILLISECONDS.toDays(result.fertileStart - now).toInt()

            // Tentukan fase saat ini
            val currentPhase = when {
                daysToNextPeriod in 0..4 || daysToNextPeriod < 0 -> "Fase Menstruasi 🩸"
                daysToFertileStart in -1..1                       -> "Masa Subur 🌟"
                daysToFertileStart in 2..5                        -> "Fase Folikuler 🌿"
                else                                               -> "Fase Luteal 🌙"
            }
            prefs.saveCurrentPhase(currentPhase)

            // ✅ Update tvCycleDay (hari tersisa)
            val daysRemaining = if (daysToNextPeriod > 0) daysToNextPeriod else 0
            findViewById<TextView>(R.id.tvCycleDay)?.text = "$daysRemaining"

            // ✅ Update tvPhaseBadge (fase aktif)
            findViewById<TextView>(R.id.tvPhaseBadge)?.text = currentPhase

            // ✅ Update tvCycleLength (panjang siklus)
            findViewById<TextView>(R.id.tvCycleLength)?.text = "${result.averageCycle}"

            Log.d("DASHBOARD", "Fase: $currentPhase | H-$daysToNextPeriod | Siklus: ${result.averageCycle}hr")
        }

        // Observe risiko kehamilan
        viewModel.isPregnancyRisk.observe(this) { isRisk ->
            if (isRisk) {
                Log.w("DASHBOARD", "⚠️ Risiko Kehamilan Terdeteksi")
            }
        }

        // ✅ Observe notifikasi → minta izin dulu sebelum jadwalkan
        viewModel.notifications.observe(this) {
            checkAndRequestNotificationPermission()
        }

        // Cek artikel edukasi (debug)
        lifecycleScope.launch {
            try {
                val articles = database.educationDao().getAllArticles().first()
                Log.d("DATABASE_CHECK", "Jumlah artikel: ${articles.size}")
            } catch (e: Exception) {
                Log.e("DATABASE_CHECK", "Error DB: ${e.message}")
            }
        }

        // Navigasi
        findViewById<android.view.View>(R.id.cardLogHarian)?.setOnClickListener {
            startActivity(Intent(this, DailyLogActivity::class.java))
        }
        findViewById<android.view.View>(R.id.navSiklus)?.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }
        findViewById<android.view.View>(R.id.navLog)?.setOnClickListener {
            startActivity(Intent(this, DailyLogActivity::class.java))
        }
        findViewById<android.view.View>(R.id.cardPartnerSync)?.setOnClickListener {
            startActivity(Intent(this, PartnerSyncActivity::class.java))
        }
    }

    /**
     * ✅ Fix masalah: Cegah duplikasi WorkManager dengan tag unik per siklus.
     * Setiap notif diberi tag unik — jika tag sudah ada, tidak akan di-enqueue ulang.
     */
    private fun scheduleNotifications() {
        viewModel.notifications.value?.let { notifications ->
            Log.d("DASHBOARD", "Menjadwalkan ${notifications.size} notifikasi (dengan dedup tag)")
            NotificationScheduler.scheduleFertilityNotifications(this, notifications)
        }
    }

    /**
     * ✅ Fix masalah: Minta izin POST_NOTIFICATIONS di runtime untuk Android 13+.
     * Untuk Android 12 ke bawah, langsung jadwalkan tanpa perlu minta izin.
     */
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Sudah ada izin → langsung jadwalkan
                    scheduleNotifications()
                }
                else -> {
                    // Minta izin ke user → hasil ditangani oleh notifPermissionLauncher
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android 12 ke bawah tidak perlu minta izin runtime
            scheduleNotifications()
        }
    }
}