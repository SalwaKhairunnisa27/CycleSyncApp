package com.example.cyclesyncapp.data.local.preferences

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("cyclesync_prefs", Context.MODE_PRIVATE)

    fun saveNickname(nickname: String) {
        prefs.edit().putString("nickname", nickname).apply()
    }

    fun getNickname(): String? {
        return prefs.getString("nickname", null)
    }

    fun saveGoal(goal: String) {
        prefs.edit().putString("goal", goal).apply()
    }

    fun getGoal(): String? {
        return prefs.getString("goal", "rutin")
    }

    fun savePartnerCode(code: String) {
        prefs.edit().putString("partner_code", code).apply()
    }

    fun getPartnerCode(): String? {
        return prefs.getString("partner_code", null)
    }

    fun savePartnerContact(name: String, phone: String) {
        prefs.edit()
            .putString("partner_name", name)
            .putString("partner_phone", phone)
            .apply()
    }

    fun getPartnerName(): String? = prefs.getString("partner_name", null)

    fun getPartnerPhone(): String? = prefs.getString("partner_phone", null)

    fun clearPartnerContact() {
        prefs.edit()
            .remove("partner_name")
            .remove("partner_phone")
            .apply()
    }

    // Simpan tanggal LMP (Last Menstrual Period) dari onboarding
    fun saveLmpDate(year: Int, month: Int, day: Int) {
        prefs.edit()
            .putInt("lmp_year", year)
            .putInt("lmp_month", month)
            .putInt("lmp_day", day)
            .apply()
    }

    fun getLmpYear(): Int = prefs.getInt("lmp_year", -1)
    fun getLmpMonth(): Int = prefs.getInt("lmp_month", -1)
    fun getLmpDay(): Int = prefs.getInt("lmp_day", -1)
    fun hasLmpDate(): Boolean = getLmpYear() != -1

    // Simpan fase siklus aktif (untuk dibagikan via PartnerSync)
    fun saveCurrentPhase(phase: String) {
        prefs.edit().putString("current_phase", phase).apply()
    }

    fun getCurrentPhase(): String {
        return prefs.getString("current_phase", "Fase Aktif") ?: "Fase Aktif"
    }

    fun saveNextPeriodDays(days: Int) {
        prefs.edit().putInt("next_period_days", days).apply()
    }

    fun getNextPeriodDays(): Int = prefs.getInt("next_period_days", -1)

    fun saveOnboardingComplete(isComplete: Boolean) {
        prefs.edit().putBoolean("onboarding_complete", isComplete).apply()
    }

    fun isOnboardingComplete(): Boolean {
        return prefs.getBoolean("onboarding_complete", false)
    }

    // ✅ Notification Settings
    fun saveNotificationSettings(dailyLog: Boolean, periodRemind: Boolean, supplement: Boolean) {
        prefs.edit().apply {
            putBoolean("notif_daily_log", dailyLog)
            putBoolean("notif_period", periodRemind)
            putBoolean("notif_supplement", supplement)
        }.apply()
    }

    fun isDailyLogNotifEnabled(): Boolean = prefs.getBoolean("notif_daily_log", true)
    fun isPeriodNotifEnabled(): Boolean = prefs.getBoolean("notif_period", true)
    fun isSupplementNotifEnabled(): Boolean = prefs.getBoolean("notif_supplement", false)
}

