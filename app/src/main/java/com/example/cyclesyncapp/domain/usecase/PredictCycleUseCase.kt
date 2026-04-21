package com.example.cyclesyncapp.domain.usecase

import java.util.Calendar

class PredictCycleUseCase {

    fun predictNextPeriod(
        lastPeriod: Calendar,
        cycleHistory: List<Int>
    ): Calendar {

        if (cycleHistory.isEmpty()) {
            throw IllegalArgumentException("Cycle history tidak boleh kosong")
        }

        // Moving Average
        val averageCycle = cycleHistory.average().toInt()

        val nextPeriod = lastPeriod.clone() as Calendar
        nextPeriod.add(Calendar.DAY_OF_MONTH, averageCycle)

        return nextPeriod
    }
}