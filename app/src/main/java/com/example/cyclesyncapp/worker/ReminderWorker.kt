package com.example.cyclesyncapp.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.cyclesyncapp.data.local.database.CycleDatabase
import com.example.cyclesyncapp.utils.NotificationHelper
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val db = CycleDatabase.getDatabase(applicationContext)
        val helper = NotificationHelper(applicationContext)

        val prefs = applicationContext.getSharedPreferences("cyclesync_prefs", Context.MODE_PRIVATE)
        val notifDailyLog = prefs.getBoolean("notif_daily_log", true)
        val notifPeriod = prefs.getBoolean("notif_period", true)
        val notifFertility = prefs.getBoolean("notif_fertility", true)
        val notifSupplement = prefs.getBoolean("notif_supplement", false)

        return runBlocking {
            // Ambil data profil user untuk mendapatkan panjang siklus asli
            val user = db.userDao().getUser()
            val avgCycleLength = user?.cycleLength ?: 28 // Fallback ke 28 hari jika null
            val ovulationOffset = avgCycleLength - 14 // Ovulasi biasanya H-14 sebelum haid berikutnya

            val cycles = db.cycleDao().getAllCycles().firstOrNull()
            val latestCycle = cycles?.firstOrNull()

            latestCycle?.let {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val startDate = sdf.parse(it.startDate)
                
                startDate?.let { date ->
                    val calendar = Calendar.getInstance().apply { time = date }
                    val today = Calendar.getInstance()
                    
                    // 1. Logika Ovulasi Dinamis (Masa Subur)
                    if (notifFertility) {
                        val ovulationDay = calendar.clone() as Calendar
                        ovulationDay.add(Calendar.DAY_OF_YEAR, ovulationOffset)
                        
                        val hMinus2 = (ovulationDay.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -2) }
                        
                        if (isSameDay(today, hMinus2)) {
                            helper.showNotification("Reminder CycleSync", "Masa suburmu akan mulai 2 hari lagi! 🌸")
                        } else if (isSameDay(today, ovulationDay)) {
                            helper.showNotification("Reminder CycleSync", "Hari ini adalah prediksi hari ovulasimu ✨")
                        }
                    }

                    // 2. Logika Tes Kehamilan / Telat Haid (>14 hari terlambat)
                    if (notifPeriod) {
                        val expectedNextPeriod = calendar.clone() as Calendar
                        expectedNextPeriod.add(Calendar.DAY_OF_YEAR, avgCycleLength)
                        
                        val lateLimit = expectedNextPeriod.clone() as Calendar
                        lateLimit.add(Calendar.DAY_OF_YEAR, 14)
                        
                        if (today.after(lateLimit)) {
                            helper.showNotification("Penting: CycleSync", "Kamu terlambat haid > 14 hari. Pertimbangkan untuk tes kehamilan.")
                        }
                    }

                    // 3. Logika Log Harian
                    if (notifDailyLog) {
                        // Tampilkan log harian pengingat
                        helper.showNotification("Catat Siklusmu", "Waktunya mengisi log harian mood & gejalamu hari ini! 🌙")
                    }

                    // 4. Logika Suplemen
                    if (notifSupplement) {
                        helper.showNotification("Pengingat Suplemen", "Jangan lupa untuk mengonsumsi suplemen harianmu sesuai fase aktif saat ini 💊")
                    }
                }
            }
            Result.success()
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}