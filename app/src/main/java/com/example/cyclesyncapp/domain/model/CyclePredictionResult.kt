package com.example.cyclesyncapp.domain.model

data class CyclePredictionResult(
    val averageCycle: Int,
    val nextPeriodDate: Long,
    val fertileStart: Long,
    val fertileEnd: Long,
    val currentPhase: HormonalPhase // Menggunakan Enum agar tipe data selaras dan aman
)
