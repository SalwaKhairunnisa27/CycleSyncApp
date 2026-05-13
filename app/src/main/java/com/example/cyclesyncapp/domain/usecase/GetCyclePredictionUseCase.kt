package com.example.cyclesyncapp.domain.usecase

import com.example.cyclesyncapp.domain.model.CyclePredictionResult
import java.util.Calendar

class GetCyclePredictionUseCase(
    private val predictCycleUseCase: PredictCycleUseCase = PredictCycleUseCase(),
    private val fertileUseCase: CalculateFertileWindowUseCase = CalculateFertileWindowUseCase()
) {

    fun execute(
        lastPeriod: Calendar,
        cycleHistory: List<Int>
    ): CyclePredictionResult {

        require(cycleHistory.isNotEmpty()) {
            "Cycle history tidak boleh kosong"
        }

        val averageCycle = cycleHistory.average().toInt()

        // Prediksi next period
        val nextPeriod = predictCycleUseCase.predictNextPeriod(lastPeriod, cycleHistory)

        // Hitung masa subur
        val (fertileStart, fertileEnd) = fertileUseCase.calculateFertileWindow(nextPeriod)

        return CyclePredictionResult(
            averageCycle = averageCycle,
            nextPeriodDate = nextPeriod.timeInMillis,
            fertileStart = fertileStart.timeInMillis,
            fertileEnd = fertileEnd.timeInMillis
        )
    }
}
