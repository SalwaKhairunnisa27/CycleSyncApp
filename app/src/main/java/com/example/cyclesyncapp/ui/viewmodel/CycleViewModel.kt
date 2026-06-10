package com.example.cyclesyncapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.cyclesyncapp.data.local.database.CycleDatabase
import com.example.cyclesyncapp.data.local.entity.CycleEntity
import com.example.cyclesyncapp.domain.model.CyclePredictionResult
import com.example.cyclesyncapp.domain.usecase.GetCyclePredictionUseCase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CycleViewModel(application: Application) : AndroidViewModel(application) {
    private val useCase = GetCyclePredictionUseCase()
    private val database = CycleDatabase.getDatabase(application)

    private val _predictionResult = MutableLiveData<CyclePredictionResult>()
    val predictionResult: LiveData<CyclePredictionResult> = _predictionResult

    private val _cycleHistory = MutableLiveData<List<CycleEntity>>()
    val cycleHistory: LiveData<List<CycleEntity>> = _cycleHistory

    init {
        // Observe database flow and post to LiveData automatically
        viewModelScope.launch {
            database.cycleDao().getAllCycles().collect { list ->
                _cycleHistory.postValue(list)
            }
        }
    }

    fun calculatePrediction(year: Int, month: Int, day: Int, cycleHistory: List<Int>, isConfirmedPregnant: Boolean = false) {
        // Forwarding call to unified prediction loader
        loadPredictionData(isConfirmedPregnant)
    }

    fun loadPredictionData(isConfirmedPregnant: Boolean = false) {
        viewModelScope.launch {
            try {
                // 1. Get onboarding LMP from SharedPreferences
                val prefs = getApplication<Application>().getSharedPreferences("cycle_sync_prefs", android.content.Context.MODE_PRIVATE)
                var onboardingLmpStr = prefs.getString("onboarding_lmp", null)
                
                if (onboardingLmpStr == null) {
                    // Fallback for existing database states: find the oldest or latest cycle
                    val latestCycle = database.cycleDao().getLatestCycle()
                    if (latestCycle != null) {
                        onboardingLmpStr = latestCycle.startDate
                        prefs.edit().putString("onboarding_lmp", onboardingLmpStr).apply()
                    }
                }
                
                val isUnknownLmp = onboardingLmpStr == null || onboardingLmpStr == "UNKNOWN_LMP"
                
                // 2. Get period logs from daily logs database
                val periodLogs = database.dailyLogDao().getPeriodLogs()
                
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val periodStarts = mutableListOf<Long>()

                // Add onboarding cycle start date first if it is known
                if (onboardingLmpStr != null && !isUnknownLmp) {
                    try {
                        sdf.parse(onboardingLmpStr)?.time?.let {
                            periodStarts.add(it)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Add period start dates from daily logs
                if (periodLogs.isNotEmpty()) {
                    val sortedLogs = periodLogs.sortedBy { it.date }
                    val logTimes = sortedLogs.mapNotNull { log ->
                        try { sdf.parse(log.date)?.time } catch (e: Exception) { null }
                    }
                    
                    // Group consecutive dates to identify the first day of each period as a start date
                    for (i in logTimes.indices) {
                        if (i == 0 || logTimes[i] > logTimes[i - 1] + (1.5 * 24.0 * 60.0 * 60.0 * 1000.0).toLong()) {
                            // If this timestamp is more than 1.5 days after the previous, it is a new period start
                            periodStarts.add(logTimes[i])
                        }
                    }
                }

                // Deduplicate start dates and sort chronologically
                val uniquePeriodStarts = periodStarts.distinct().sorted()

                // 3. Recalculate cycle history list and write to Room database
                val allLogs = database.dailyLogDao().getAllLogsList()
                val newCycles = mutableListOf<CycleEntity>()
                
                // Compute average cycle length first (or use 28)
                val cycleLengths = mutableListOf<Int>()
                for (i in 0 until uniquePeriodStarts.size - 1) {
                    val diffMs = uniquePeriodStarts[i + 1] - uniquePeriodStarts[i]
                    val diffDays = (diffMs / (24 * 60 * 60 * 1000)).toInt()
                    if (diffDays in 10..60) {
                        cycleLengths.add(diffDays)
                    }
                }
                val avgCycle = if (cycleLengths.isNotEmpty()) cycleLengths.average().toInt() else 28

                for (i in uniquePeriodStarts.indices) {
                    val startMs = uniquePeriodStarts[i]
                    val startStr = sdf.format(Date(startMs))
                    
                    val calculatedLength = if (i < uniquePeriodStarts.size - 1) {
                        val diffMs = uniquePeriodStarts[i + 1] - startMs
                        (diffMs / (24 * 60 * 60 * 1000)).toInt()
                    } else {
                        avgCycle
                    }
                    
                    val endMs = startMs + calculatedLength.toLong() * 24 * 60 * 60 * 1000
                    val endStr = sdf.format(Date(endMs))

                    // Aggregate notes/symptoms for this cycle range: [startMs, nextStartMs)
                    val nextStartMs = if (i < uniquePeriodStarts.size - 1) uniquePeriodStarts[i + 1] else Long.MAX_VALUE
                    val logsInCycle = allLogs.filter { log ->
                        val logTime = try { sdf.parse(log.date)?.time } catch(e: Exception) { null }
                        logTime != null && logTime >= startMs && logTime < nextStartMs
                    }
                    
                    val symptomsList = logsInCycle.flatMap { it.symptoms?.split(",")?.map { s -> s.trim() } ?: emptyList() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                        
                    val notesList = logsInCycle.mapNotNull { it.encryptedNote }
                        .map { note ->
                            try {
                                com.example.cyclesyncapp.data.security.EncryptionManager.decrypt(note)
                            } catch (e: Exception) {
                                note
                            }
                        }
                        .filter { it.isNotEmpty() && it != "Dummy note" }
                        .distinct()
                        
                    val summaryParts = mutableListOf<String>()
                    if (symptomsList.isNotEmpty()) {
                        summaryParts.add("Gejala: ${symptomsList.joinToString(", ")}")
                    }
                    if (notesList.isNotEmpty()) {
                        summaryParts.add("Catatan: ${notesList.joinToString("; ")}")
                    }
                    val cycleNotes = if (summaryParts.isNotEmpty()) summaryParts.joinToString(" | ") else "Tidak ada catatan"
                    
                    newCycles.add(
                        CycleEntity(
                            startDate = startStr,
                            endDate = endStr,
                            cycleLength = calculatedLength,
                            periodLength = 5,
                            notes = cycleNotes
                        )
                    )
                }

                // Update the cycle_table database with the rebuilt list
                database.cycleDao().clearCycles()
                database.cycleDao().insertCycles(newCycles)

                // 4. Calculate prediction
                val result = useCase.execute(uniquePeriodStarts, isConfirmedPregnant)
                _predictionResult.postValue(result)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}