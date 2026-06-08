package com.example.cyclesyncapp.domain.usecase

import com.example.cyclesyncapp.domain.model.CyclePredictionResult
import java.util.Calendar

class GetCyclePredictionUseCase(
    private val predictCycleUseCase: PredictCycleUseCase = PredictCycleUseCase()
) {

    fun execute(
        lastPeriod: Calendar,
        cycleHistory: List<Int>
    ): CyclePredictionResult {

        require(cycleHistory.isNotEmpty()) {
            "Cycle history tidak boleh kosong"
        }

        return predictCycleUseCase.execute(
            lastPeriod,
            cycleHistory
        )
    }
}