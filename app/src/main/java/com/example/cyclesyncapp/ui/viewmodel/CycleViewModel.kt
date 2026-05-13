package com.example.cyclesyncapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.cyclesyncapp.domain.usecase.GetCyclePredictionUseCase
import com.example.cyclesyncapp.domain.model.CyclePredictionResult
import com.example.cyclesyncapp.domain.usecase.DetectPregnancyRiskUseCase
import com.example.cyclesyncapp.domain.usecase.ScheduleGoalNotificationUseCase
import com.example.cyclesyncapp.domain.usecase.PartnerSyncUseCase
import com.example.cyclesyncapp.domain.usecase.NotificationSettings
import com.example.cyclesyncapp.domain.usecase.NotificationData
import com.example.cyclesyncapp.domain.usecase.UserGoal
import java.util.Calendar

class CycleViewModel : ViewModel() {
    private val useCase = GetCyclePredictionUseCase()
    private val pregnancyRiskUseCase = DetectPregnancyRiskUseCase()
    private val notificationUseCase = ScheduleGoalNotificationUseCase()
    private val partnerSyncUseCase = PartnerSyncUseCase()

    private val _predictionResult = MutableLiveData<CyclePredictionResult>()
    val predictionResult: LiveData<CyclePredictionResult> = _predictionResult

    private val _isPregnancyRisk = MutableLiveData<Boolean>()
    val isPregnancyRisk: LiveData<Boolean> = _isPregnancyRisk

    private val _notifications = MutableLiveData<List<NotificationData>>()
    val notifications: LiveData<List<NotificationData>> = _notifications

    private val _partnerSyncCode = MutableLiveData<String>()
    val partnerSyncCode: LiveData<String> = _partnerSyncCode

    private val _syncStatus = MutableLiveData<Boolean>()
    val syncStatus: LiveData<Boolean> = _syncStatus

    fun calculatePrediction(
        year: Int, 
        month: Int, 
        day: Int, 
        cycleHistory: List<Int>, 
        goal: UserGoal = UserGoal.TRACK_CYCLE,
        settings: NotificationSettings? = null
    ) {
        try {
            val lastPeriod = Calendar.getInstance().apply { set(year, month, day) }
            val result = useCase.execute(lastPeriod, cycleHistory)
            _predictionResult.value = result

            // Periksa risiko kehamilan jika siklus terlambat
            val nextPeriod = Calendar.getInstance().apply { timeInMillis = result.nextPeriodDate }
            _isPregnancyRisk.value = pregnancyRiskUseCase.detectPotentialPregnancy(nextPeriod)

            // Buat notifikasi jika settings tersedia
            if (settings != null) {
                val fertileStart = Calendar.getInstance().apply { timeInMillis = result.fertileStart }
                val fertileEnd = Calendar.getInstance().apply { timeInMillis = result.fertileEnd }
                _notifications.value = notificationUseCase.generateAllNotifications(
                    goal, fertileStart, fertileEnd, nextPeriod, settings
                )
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun generatePartnerCode(userId: String) {
        _partnerSyncCode.value = partnerSyncUseCase.generateSyncCode(userId)
    }

    fun syncWithPartner(partnerCode: String) {
        _syncStatus.value = partnerSyncUseCase.syncWithPartner(partnerCode)
    }
}
