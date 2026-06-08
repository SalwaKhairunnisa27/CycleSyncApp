package com.example.cyclesyncapp.domain.usecase


import com.example.cyclesyncapp.domain.model.CyclePredictionResult
import java.util.Calendar

class PredictCycleUseCase {

    fun execute(lastPeriod: Calendar, cycleHistory: List<Int>): CyclePredictionResult {
        // 1. Moving Average Adaptif: Ambil maksimal 6 siklus terakhir
        // Supaya prediksi lebih peka terhadap perubahan kondisi tubuh terbaru pengguna.
        val recentHistory = cycleHistory.takeLast(6)

        val averageCycle = if (recentHistory.isNotEmpty()) {
            recentHistory.average().toInt()
        } else {
            28 // Default standar jika data masih kosong
        }

        // 2. Hitung Haid Berikutnya
        val nextPeriod = lastPeriod.clone() as Calendar
        nextPeriod.add(Calendar.DAY_OF_MONTH, averageCycle)

        // 3. Hitung Estimasi Masa Subur (Fertile Window)
        // Umumnya: Ovulasi terjadi hari ke-14 SEBELUM haid berikutnya.
        // Masa subur adalah rentang sekitar hari ovulasi tersebut.
        val fertileStart = nextPeriod.clone() as Calendar
        fertileStart.add(Calendar.DAY_OF_MONTH, -16)

        val fertileEnd = nextPeriod.clone() as Calendar
        fertileEnd.add(Calendar.DAY_OF_MONTH, -11)

        return CyclePredictionResult(
            averageCycle = averageCycle,
            nextPeriodDate = nextPeriod.timeInMillis,
            fertileStart = fertileStart.timeInMillis,
            fertileEnd = fertileEnd.timeInMillis
        )
    }
}