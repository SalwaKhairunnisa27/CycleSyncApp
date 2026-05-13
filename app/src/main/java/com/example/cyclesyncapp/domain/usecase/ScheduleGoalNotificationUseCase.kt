package com.example.cyclesyncapp.domain.usecase

import java.util.Calendar

enum class UserGoal {
    TRACK_CYCLE,
    AVOID_PREGNANCY,
    GET_PREGNANT
}

data class NotificationSettings(
    val dailyLogEnabled: Boolean,
    val periodRemindEnabled: Boolean,
    val supplementEnabled: Boolean
)

data class NotificationData(val title: String, val message: String, val triggerTimeInMillis: Long)

class ScheduleGoalNotificationUseCase {

    fun generateAllNotifications(
        goal: UserGoal,
        fertileStart: Calendar,
        fertileEnd: Calendar,
        nextPeriod: Calendar?,
        settings: NotificationSettings
    ): List<NotificationData> {
        val notifications = mutableListOf<NotificationData>()

        // 1. ✅ Notifikasi Siklus (Hanya jika diizinkan user)
        if (settings.periodRemindEnabled && nextPeriod != null) {
            // H-3 sebelum haid
            val h3 = nextPeriod.clone() as Calendar
            h3.add(Calendar.DAY_OF_MONTH, -3)
            h3.set(Calendar.HOUR_OF_DAY, 9)
            h3.set(Calendar.MINUTE, 0)
            if (h3.timeInMillis > System.currentTimeMillis()) {
                notifications.add(NotificationData(
                    "🗓️ Pengingat Siklus",
                    "Haidmu diperkirakan 3 hari lagi. Siapkan diri ya!",
                    h3.timeInMillis
                ))
            }

            // Masa Subur berdasarkan Goal
            when (goal) {
                UserGoal.GET_PREGNANT -> {
                    notifications.add(NotificationData(
                        "🌟 Masa Subur Dimulai!",
                        "Waktu terbaik untuk konsepsi. Semangat program hamil! 💕",
                        fertileStart.timeInMillis
                    ))
                    val peak = fertileStart.clone() as Calendar
                    peak.add(Calendar.DAY_OF_MONTH, 3)
                    notifications.add(NotificationData("⭐ Puncak Masa Subur!", "Hari ini perkiraan ovulasi!", peak.timeInMillis))
                }
                UserGoal.AVOID_PREGNANCY -> {
                    notifications.add(NotificationData(
                        "⚠️ Perhatian: Masa Subur",
                        "Gunakan perlindungan ekstra selama 7 hari ke depan.",
                        fertileStart.timeInMillis
                    ))
                }
                UserGoal.TRACK_CYCLE -> {
                    notifications.add(NotificationData("📊 Info Siklus", "Fase masa suburmu sedang berlangsung.", fertileStart.timeInMillis))
                }
            }
        }

        // 2. ✅ Notifikasi Log Harian (Setiap hari jam 08:00)
        if (settings.dailyLogEnabled) {
            val daily = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (timeInMillis < System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
            }
            notifications.add(NotificationData(
                "📝 Catat Kondisimu",
                "Bagaimana perasaanmu hari ini? Jangan lupa catat log harianmu!",
                daily.timeInMillis
            ))
        }

        // 3. ✅ Notifikasi Suplemen (Setiap hari jam 20:00 - Biasanya untuk Promil)
        if (settings.supplementEnabled) {
            val evening = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 20)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (timeInMillis < System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
            }
            notifications.add(NotificationData(
                "💊 Waktunya Suplemen",
                "Jangan lupa minum vitamin atau suplemenmu hari ini ya!",
                evening.timeInMillis
            ))
        }

        return notifications
    }
}
