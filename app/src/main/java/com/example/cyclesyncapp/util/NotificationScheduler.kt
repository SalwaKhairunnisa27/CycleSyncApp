package com.example.cyclesyncapp.util

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cyclesyncapp.domain.usecase.NotificationData
import com.example.cyclesyncapp.worker.FertilityNotificationWorker
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    fun scheduleFertilityNotifications(context: Context, notifications: List<NotificationData>) {
        val workManager = WorkManager.getInstance(context)
        val currentTime = System.currentTimeMillis()

        for (notification in notifications) {
            val delayInMillis = notification.triggerTimeInMillis - currentTime

            // Hanya jadwalkan jika waktu notifikasinya ada di masa depan
            if (delayInMillis > 0) {
                val inputData = Data.Builder()
                    .putString("title", notification.title)
                    .putString("message", notification.message)
                    .build()

                val workRequest = OneTimeWorkRequestBuilder<FertilityNotificationWorker>()
                    .setInitialDelay(delayInMillis, TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .build()

                // ✅ Gunakan tag unik berdasarkan trigger time agar tidak duplikat
                // ExistingWorkPolicy.KEEP = jika sudah terjadwal, TIDAK dijadwalkan ulang
                val uniqueTag = "notif_${notification.triggerTimeInMillis}"
                workManager.enqueueUniqueWork(
                    uniqueTag,
                    ExistingWorkPolicy.KEEP,
                    workRequest
                )
            }
        }
    }
}
