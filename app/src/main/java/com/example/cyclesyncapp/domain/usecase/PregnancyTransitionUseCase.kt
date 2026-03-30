package com.example.cyclesyncapp.domain.usecase

class PregnancyTransitionUseCase {

    fun resetCycleAfterPregnancy(): List<Int> {
        // default siklus normal setelah kehamilan
        return listOf(28, 28, 28)
    }
}