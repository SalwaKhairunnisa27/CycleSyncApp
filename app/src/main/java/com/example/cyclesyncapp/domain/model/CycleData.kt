package com.example.cyclesyncapp.domain.model

data class CycleData(
    val lastPeriodDate: String,
    val cycleLength: Int = 28,
    val periodDuration: Int = 5,
    val goal: String
)