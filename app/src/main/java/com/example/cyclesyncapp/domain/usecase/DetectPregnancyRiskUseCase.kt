package com.example.cyclesyncapp.domain.usecase

import java.util.Calendar

class DetectPregnancyRiskUseCase {

    fun detectPotentialPregnancy(
        nextExpectedPeriod: Calendar,
        currentDate: Calendar = Calendar.getInstance(),
        delayDaysThreshold: Int = 7
    ): Boolean {
        val diffInMillis = currentDate.timeInMillis - nextExpectedPeriod.timeInMillis
        val daysLate = diffInMillis / (1000 * 60 * 60 * 24)
        
        return daysLate >= delayDaysThreshold
    }
}
