package com.example.cyclesyncapp.domain.model

data class CyclePredictionResult(
    val averageCycle: Int,
    val dayOfCycle: Int,
    val nextPeriodDate: Long,
    val fertileStart: Long,
    val fertileEnd: Long,
    val currentPhase: HormonalPhase,
    
    // New fields for accuracy and regularity
    val entryCount: Int = 0,
    val standardDeviation: Double = 0.0,
    val confidenceLevel: String = "Data Terbatas", // "Data Terbatas", "Cukup Akurat", "Sangat Akurat"
    val regularityStatus: String = "Belum Diketahui", // "Teratur", "Cukup Teratur", "Tidak Teratur", "Belum Diketahui"
    val disclaimerText: String = "",
    val progressText: String = "",
    val progressPercent: Int = 0,
    val hasPrediction: Boolean = false,
    val predictionRangeStart: Long = 0L,
    val predictionRangeEnd: Long = 0L
)
