package com.example.cyclesyncapp.domain.usecase

import java.util.Calendar

data class CyclePredictionResult(
    val nextPeriod: Calendar,
    val fertileStart: Calendar,
    val fertileEnd: Calendar,
    val ovulationDay: Calendar
)

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

        // Prediksi next period pakai Moving Average
        val nextPeriod =
            predictCycleUseCase.predictNextPeriod(lastPeriod, cycleHistory)

        // Hitung masa subur
        val (fertileStart, fertileEnd) =
            fertileUseCase.calculateFertileWindow(nextPeriod)

        // Ovulasi = 14 hari sebelum next period
        val ovulation = nextPeriod.clone() as Calendar
        ovulation.add(Calendar.DAY_OF_MONTH, -14)

        return CyclePredictionResult(
            nextPeriod = nextPeriod,
            fertileStart = fertileStart,
            fertileEnd = fertileEnd,
            ovulationDay = ovulation
        )
    }
}