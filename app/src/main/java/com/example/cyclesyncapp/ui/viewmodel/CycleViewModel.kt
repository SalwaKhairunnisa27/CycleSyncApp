package com.example.cyclesyncapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.cyclesyncapp.domain.usecase.GetCyclePredictionUseCase
import com.example.cyclesyncapp.domain.usecase.CyclePredictionResult
import java.util.Calendar

class CycleViewModel : ViewModel() {

    private val useCase = GetCyclePredictionUseCase()

    // 🔹 LiveData untuk dikirim ke UI
    private val _predictionResult = MutableLiveData<CyclePredictionResult>()
    val predictionResult: LiveData<CyclePredictionResult> = _predictionResult

    /**
     * 🔹 Versi lama (biar MainActivity kamu sekarang tetap jalan)
     */
    fun getPrediction(): CyclePredictionResult {
        val lastPeriod = Calendar.getInstance()
        lastPeriod.set(2026, 3, 1)

        val cycleHistory = listOf(28, 30, 27)

        return useCase.execute(lastPeriod, cycleHistory)
    }

    /**
     * 🔹 Versi baru (BEST PRACTICE - dipakai UI nanti)
     */
    fun calculatePrediction(
        year: Int,
        month: Int,
        day: Int,
        cycleHistory: List<Int>
    ) {
        try {
            val lastPeriod = Calendar.getInstance()
            lastPeriod.set(year, month, day)

            val result = useCase.execute(lastPeriod, cycleHistory)

            _predictionResult.value = result

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}