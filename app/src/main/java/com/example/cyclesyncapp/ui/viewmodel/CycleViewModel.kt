package com.example.cyclesyncapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.cyclesyncapp.domain.usecase.GetCyclePredictionUseCase
import com.example.cyclesyncapp.domain.model.CyclePredictionResult // Sudah saya perbaiki lokasinya
import java.util.Calendar

class CycleViewModel : ViewModel() {
    private val useCase = GetCyclePredictionUseCase()

    private val _predictionResult = MutableLiveData<CyclePredictionResult>()
    val predictionResult: LiveData<CyclePredictionResult> = _predictionResult

    fun calculatePrediction(year: Int, month: Int, day: Int, cycleHistory: List<Int>) {
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