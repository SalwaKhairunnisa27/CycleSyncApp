package com.example.cyclesyncapp.domain.usecase

import com.example.cyclesyncapp.domain.model.CyclePredictionResult
import com.example.cyclesyncapp.domain.model.HormonalPhase
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * UseCase untuk menghitung prediksi siklus dan menentukan fase hormonal secara dinamis.
 *  untuk mendukung siklus yang tidak selalu 28 hari.
 */
class GetCyclePredictionUseCase(
    private val predictCycleUseCase: PredictCycleUseCase = PredictCycleUseCase(),
    private val fertileUseCase: CalculateFertileWindowUseCase = CalculateFertileWindowUseCase()
) {

    fun execute(
        lastPeriod: Calendar,
        cycleHistory: List<Int>,
        isConfirmedPregnant: Boolean = false
    ): CyclePredictionResult {

        // 1. Jika user sudah konfirmasi hamil, langsung kembalikan fase PREGNANCY
        if (isConfirmedPregnant) {
            return buildResult(lastPeriod, cycleHistory, HormonalPhase.PREGNANCY)
        }

        val averageCycle = if (cycleHistory.isNotEmpty()) cycleHistory.average().toInt() else 28
        val today = Calendar.getInstance()

        // 2. Hitung sudah berapa hari sejak hari pertama haid terakhir
        val diffInMs = today.timeInMillis - lastPeriod.timeInMillis
        val daysSinceStart = TimeUnit.MILLISECONDS.toDays(diffInMs).toInt() + 1

        // 3. LOGIKA DINAMIS: Menentukan hari Ovulasi berdasarkan panjang siklus rata-rata
        // Ovulasi biasanya terjadi 14 hari sebelum periode berikutnya dimulai
        val ovulationDay = averageCycle - 14

        // 4. Rule-Based System yang adaptif terhadap panjang siklus user
        val phase = when {
            // Jika telat haid lebih dari 14 hari dari rata-rata -> Potensi Hamil
            daysSinceStart > (averageCycle + 14) -> HormonalPhase.PREGNANCY

            // Hari 1 - 5: Fase Menstruasi (Estrogen & Progesteron rendah)
            daysSinceStart in 1..5 -> HormonalPhase.MENSTRUATION

            // Dari hari ke-6 sampai sehari sebelum Ovulasi: Fase Folikuler
            daysSinceStart in 6 until ovulationDay -> HormonalPhase.FOLLICULAR

            // Tepat di hari Ovulasi: Fase Ovulasi (Estrogen mencapai puncak)
            daysSinceStart == ovulationDay -> HormonalPhase.OVULATION

            // Setelah Ovulasi sampai hari terakhir siklus: Fase Luteal
            else -> HormonalPhase.LUTEAL
        }

        return buildResult(lastPeriod, cycleHistory, phase)
    }

    private fun buildResult(
        lastPeriod: Calendar,
        cycleHistory: List<Int>,
        phase: HormonalPhase
    ): CyclePredictionResult {
        val nextPeriod = predictCycleUseCase.predictNextPeriod(lastPeriod, cycleHistory)
        val (fertileStart, fertileEnd) = fertileUseCase.calculateFertileWindow(nextPeriod)

        return CyclePredictionResult(
            averageCycle = if (cycleHistory.isNotEmpty()) cycleHistory.average().toInt() else 28,
            nextPeriodDate = nextPeriod.timeInMillis,
            fertileStart = fertileStart.timeInMillis,
            fertileEnd = fertileEnd.timeInMillis,
            currentPhase = phase
        )
    }
}