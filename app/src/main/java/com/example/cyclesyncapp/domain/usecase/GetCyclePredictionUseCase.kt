package com.example.cyclesyncapp.domain.usecase

import java.util.Calendar
import com.example.cyclesyncapp.domain.model.CyclePredictionResult

class GetCyclePredictionUseCase(
    private val predictUseCase: PredictCycleUseCase,
    private val fertileUseCase: CalculateFertileWindowUseCase
) {

    fun execute(cycles: List<Int>, lastPeriod: Calendar): CyclePredictionResult {

        val avg = predictUseCase.calculateAverageCycle(cycles)
        val nextPeriod = predictUseCase.predictNextPeriod(lastPeriod, avg)

        val (fertileStart, fertileEnd) =
            fertileUseCase.calculateFertileWindow(nextPeriod)

        return CyclePredictionResult(
            averageCycle = avg,
            nextPeriodDate = nextPeriod.timeInMillis,
            fertileStart = fertileStart.timeInMillis,
            fertileEnd = fertileEnd.timeInMillis
        )
    }
}