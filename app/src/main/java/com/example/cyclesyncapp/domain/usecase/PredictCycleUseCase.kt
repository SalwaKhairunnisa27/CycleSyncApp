package com.example.cyclesyncapp.domain.usecase

import java.util.Calendar

class PredictCycleUseCase {

    fun calculateAverageCycle(cycles: List<Int>): Int {
        if (cycles.isEmpty()) return 28
        if (cycles.any { it <= 0 }) return 28

        return cycles.sum() / cycles.size
    }

    fun predictNextPeriod(lastPeriod: Calendar, averageCycle: Int): Calendar {
        val result = lastPeriod.clone() as Calendar
        result.add(Calendar.DAY_OF_MONTH, averageCycle)
        return result
    }
}