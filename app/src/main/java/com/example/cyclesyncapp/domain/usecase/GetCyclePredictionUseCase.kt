package com.example.cyclesyncapp.domain.usecase

import com.example.cyclesyncapp.domain.model.CyclePredictionResult
import com.example.cyclesyncapp.domain.model.HormonalPhase
import java.util.Calendar
import java.util.concurrent.TimeUnit

class GetCyclePredictionUseCase {

    fun execute(
        periodStartTimestamps: List<Long>, // List of start timestamps in milliseconds (sorted ascending)
        isConfirmedPregnant: Boolean = false
    ): CyclePredictionResult {

        // 1. If pregnant, return pregnancy state
        if (isConfirmedPregnant) {
            val lastPeriod = Calendar.getInstance()
            if (periodStartTimestamps.isNotEmpty()) {
                lastPeriod.timeInMillis = periodStartTimestamps.sorted().last()
            }
            return buildPregnancyResult(lastPeriod, periodStartTimestamps.size)
        }

        val sortedStarts = periodStartTimestamps.sorted()
        val N = sortedStarts.size

        // Case 1: Minimum data requirements not met (< 2 logged dates)
        if (N < 2) {
            val lastPeriodTimestamp = sortedStarts.lastOrNull()
            val rawDays = if (lastPeriodTimestamp != null) {
                val diffMs = Calendar.getInstance().timeInMillis - lastPeriodTimestamp
                (java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMs).toInt() + 1).coerceAtLeast(1)
            } else {
                1
            }
            
            // Normalize raw day for phase mapping (e.g. wrap at 28 days)
            val dayOfCycle = if (lastPeriodTimestamp != null) ((rawDays - 1) % 28) + 1 else 1
            
            val phase = if (lastPeriodTimestamp != null) {
                val ovulationDayNumber = 14
                when {
                    dayOfCycle in 1..5 -> HormonalPhase.MENSTRUATION
                    dayOfCycle in 6 until (ovulationDayNumber - 1) -> HormonalPhase.FOLLICULAR
                    dayOfCycle in (ovulationDayNumber - 1)..(ovulationDayNumber + 1) -> HormonalPhase.OVULATION
                    else -> HormonalPhase.LUTEAL
                }
            } else {
                HormonalPhase.MENSTRUATION
            }

            return CyclePredictionResult(
                averageCycle = 28,
                dayOfCycle = dayOfCycle,
                nextPeriodDate = if (lastPeriodTimestamp != null) lastPeriodTimestamp + 28L * 24 * 60 * 60 * 1000 else 0L,
                fertileStart = 0L,
                fertileEnd = 0L,
                currentPhase = phase,
                entryCount = N,
                standardDeviation = 0.0,
                confidenceLevel = "Data Kurang",
                regularityStatus = "Belum Diketahui",
                disclaimerText = "Silakan catat minimal 2 haid untuk melihat prediksi.",
                progressText = "0 dari 6 siklus tercatat untuk prediksi akurat",
                progressPercent = 0,
                hasPrediction = false,
                predictionRangeStart = 0L,
                predictionRangeEnd = 0L
            )
        }

        // Case 2: Predict based on logged period starts
        val cycleLengths = mutableListOf<Int>()
        for (i in 0 until N - 1) {
            val diffMs = sortedStarts[i + 1] - sortedStarts[i]
            val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs).toInt()
            if (diffDays in 10..60) { // filter out duplicate entries or outlier cycle lengths
                cycleLengths.add(diffDays)
            }
        }

        val avgCycle = if (cycleLengths.isNotEmpty()) cycleLengths.average() else 28.0
        val avgCycleInt = avgCycle.toInt()
        val sd = calculateSD(cycleLengths, avgCycle)
        val rangeDays = Math.max(sd, 2.0).toInt()

        val lastLmpCal = Calendar.getInstance().apply { timeInMillis = sortedStarts.last() }
        val nextPeriod = lastLmpCal.clone() as Calendar
        nextPeriod.add(Calendar.DAY_OF_YEAR, avgCycleInt)

        val predictionRangeStart = nextPeriod.clone() as Calendar
        predictionRangeStart.add(Calendar.DAY_OF_YEAR, -rangeDays)

        val predictionRangeEnd = nextPeriod.clone() as Calendar
        predictionRangeEnd.add(Calendar.DAY_OF_YEAR, rangeDays)

        val ovulation = nextPeriod.clone() as Calendar
        ovulation.add(Calendar.DAY_OF_YEAR, -14)

        val fertileStart = ovulation.clone() as Calendar
        fertileStart.add(Calendar.DAY_OF_YEAR, -5)

        val fertileEnd = ovulation.clone() as Calendar
        fertileEnd.add(Calendar.DAY_OF_YEAR, 1)

        val today = Calendar.getInstance()
        val diffMs = today.timeInMillis - sortedStarts.last()
        val rawDaysOfCycle = (TimeUnit.MILLISECONDS.toDays(diffMs).toInt() + 1).coerceAtLeast(1)
        val dayOfCycle = ((rawDaysOfCycle - 1) % avgCycleInt) + 1

        val ovulationDayNumber = avgCycleInt - 14
        val phase = when {
            dayOfCycle in 1..5 -> HormonalPhase.MENSTRUATION
            dayOfCycle in 6 until (ovulationDayNumber - 1) -> HormonalPhase.FOLLICULAR
            dayOfCycle in (ovulationDayNumber - 1)..(ovulationDayNumber + 1) -> HormonalPhase.OVULATION
            else -> HormonalPhase.LUTEAL
        }

        // Determine confidence levels and regularity status
        val cyclesCount = cycleLengths.size
        var confidence = "Data Terbatas"
        var regularity = "Belum Diketahui"
        var disclaimer = "Prediksi berdasarkan data terbatas."
        val progressText = if (cyclesCount >= 6) {
            "6 dari 6 siklus tercatat (Kondisi Optimal)"
        } else {
            "$cyclesCount dari 6 siklus tercatat untuk prediksi akurat"
        }
        val progressPercent = ((cyclesCount.toFloat() / 6f) * 100f).toInt().coerceAtIn(0, 100)

        if (N < 4) {
            confidence = "Data Terbatas"
            regularity = "Belum Diketahui"
            disclaimer = "Prediksi berdasarkan data terbatas."
        } else {
            // N >= 4 entries (at least 3 cycles computed)
            if (N >= 7) {
                confidence = "Sangat Akurat"
            } else {
                confidence = "Cukup Akurat"
            }
            disclaimer = "Prediksi ini adalah estimasi. Konsultasikan dengan dokter untuk saran medis."

            if (sd <= 2.0) {
                regularity = "Teratur"
            } else if (sd <= 5.0) {
                regularity = "Cukup Teratur"
            } else {
                regularity = "Tidak Teratur"
                disclaimer = "Siklus Anda tidak teratur. Prediksi mungkin kurang akurat. Hubungi dokter jika fluktuasi sangat tinggi."
            }
        }

        return CyclePredictionResult(
            averageCycle = avgCycleInt,
            dayOfCycle = dayOfCycle,
            nextPeriodDate = nextPeriod.timeInMillis,
            fertileStart = fertileStart.timeInMillis,
            fertileEnd = fertileEnd.timeInMillis,
            currentPhase = phase,
            entryCount = N,
            standardDeviation = sd,
            confidenceLevel = confidence,
            regularityStatus = regularity,
            disclaimerText = disclaimer,
            progressText = progressText,
            progressPercent = progressPercent,
            hasPrediction = true,
            predictionRangeStart = predictionRangeStart.timeInMillis,
            predictionRangeEnd = predictionRangeEnd.timeInMillis
        )
    }

    private fun calculateSD(lengths: List<Int>, average: Double): Double {
        if (lengths.size < 2) return 0.0
        var sum = 0.0
        for (len in lengths) {
            sum += Math.pow(len.toDouble() - average, 2.0)
        }
        return Math.sqrt(sum / lengths.size.toDouble())
    }

    private fun buildPregnancyResult(lastPeriod: Calendar, count: Int): CyclePredictionResult {
        val today = Calendar.getInstance()
        val diffInMillis = today.timeInMillis - lastPeriod.timeInMillis
        val totalDays = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt().coerceAtLeast(0)
        val weeks = totalDays / 7
        val days = totalDays % 7

        val hplCal = lastPeriod.clone() as Calendar
        hplCal.add(Calendar.DAY_OF_YEAR, 280)

        return CyclePredictionResult(
            averageCycle = 28,
            dayOfCycle = totalDays,
            nextPeriodDate = hplCal.timeInMillis, // Estimated HPL
            fertileStart = 0L,
            fertileEnd = 0L,
            currentPhase = HormonalPhase.PREGNANCY,
            entryCount = count,
            standardDeviation = 0.0,
            confidenceLevel = "Mode Kehamilan",
            regularityStatus = "Teratur",
            disclaimerText = "Tetap sehat & bugar selama masa kehamilan. Konsultasikan ke dokter kandungan Anda.",
            progressText = "Pelacakan Kehamilan Aktif",
            progressPercent = 100,
            hasPrediction = true,
            predictionRangeStart = hplCal.timeInMillis,
            predictionRangeEnd = hplCal.timeInMillis
        )
    }
}

// Helper extension function
private fun Int.coerceAtIn(minimumValue: Int, maximumValue: Int): Int {
    if (this < minimumValue) return minimumValue
    if (this > maximumValue) return maximumValue
    return this
}